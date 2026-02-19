package com.ssafy.withy.domain.content.service;

import com.ssafy.withy.domain.content.entity.Content;
import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.content.entity.MediaType;
import com.ssafy.withy.domain.content.repository.ContentRepository;
import com.ssafy.withy.domain.content.repository.GenreRepository;
import com.ssafy.withy.domain.party.entity.PlatformType;
import com.ssafy.withy.global.api.tmdb.TmdbApiClient;
import com.ssafy.withy.global.api.youtube.YoutubeApiClient;
import com.ssafy.withy.global.api.youtube.dto.YoutubeVideoInfo;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final GenreRepository genreRepository;
    private final TmdbApiClient tmdbApiClient;
    private final YoutubeApiClient youtubeApiClient;
    @Value("${spring.cloud.aws.s3.base-url}")
    private String s3BaseUrl;
    private static final String DEFAULT_POSTER_PATH = "/poster/default_poster.png";

    @Transactional
    public Content getOrCreateContent(String externalId, String contentTitle, PlatformType platform) {

        // 1. [DB] External ID로 조회 (이미 등록된 컨텐츠면 바로 반환)
        Optional<Content> byId = contentRepository.findByExternalId(externalId);
        if (byId.isPresent()) {
            Content content = byId.get();

            if (platform == PlatformType.OTT && content.getContentGenres().isEmpty()) {
                updateOttContentDetails(content, content.getTmdbId());
            }
            return content;
        }

        // 2. [YOUTUBE] 유튜브는 바로 생성.
        if (platform == PlatformType.YOUTUBE) {
            return createYoutubeContent(externalId, contentTitle);
        }

        // ---------------- [NETFLIX (OTT) 로직] ----------------

        // 3. [DB] 요청 온 제목으로 재조회 (혹시 ID 없이 등록된 데이터 있나 확인)
        Optional<Content> byTitle = contentRepository.findByOriginalTitle(contentTitle);
        if (byTitle.isPresent()) {
            Content content = byTitle.get();
            content.linkWithNetflix(externalId, contentTitle); // ID 매핑
            updateOttContentDetails(content, content.getTmdbId());
            return content;
        }

        // 4. [API] TMDB API 조회 (실패 시 Fallback)
        TmdbApiClient.TmdbInfo tmdbInfo = null;
        try {
            tmdbInfo = tmdbApiClient.searchContent(contentTitle);
        } catch (Exception e) {
            log.warn("TMDB Search Failed for title: {}. Creating content with default info.", contentTitle);
            // 검색 실패 시 Fallback: TMDB 정보 없이 기본 컨텐츠 생성
            return createFallbackContent(externalId, contentTitle);
        }

        // 5. [DB] TMDB ID로 최종 확인
        if (tmdbInfo != null) {
            Optional<Content> byTmdbId = contentRepository.findByTmdbId(tmdbInfo.getTmdbId());
            if (byTmdbId.isPresent()) {
                Content content = byTmdbId.get();
                content.linkWithNetflix(externalId, contentTitle);
                updateOttContentDetails(content, content.getTmdbId());
                return content;
            }

            // 6. [Create] 진짜 없음 (TMDB 정보로 생성)
            return createNetflixContent(externalId, contentTitle, tmdbInfo);
        }

        return createFallbackContent(externalId, contentTitle);
    }

    private Content createFallbackContent(String externalId, String contentTitle) {
        Content content = Content.builder()
                .externalId(externalId)
                .title(contentTitle)
                .originalTitle(contentTitle)
                .mediaType(MediaType.MOVIE) // 기본값 설정 (추후 수정 가능)
                .posterPath(s3BaseUrl + DEFAULT_POSTER_PATH)
                .build();
        return contentRepository.save(content);
    }

    private Content createYoutubeContent(String externalId, String title) { // title은 혹시 API 실패 시 fallback용으로 남겨둠

        String finalTitle = title;
        String posterPath = "https://img.youtube.com/vi/" + externalId + "/hqdefault.jpg"; // 기본 썸네일 (Fallback)

        // 1. API 호출 시도 (제목, 썸네일, 장르ID 가져오기)
        try {
            // [변경] 메서드 이름 변경 & 리턴 타입 변경
            YoutubeVideoInfo videoInfo = youtubeApiClient.fetchVideoInfo(externalId);

            // API에서 가져온 확실한 정보로 덮어쓰기
            finalTitle = videoInfo.title();
            posterPath = videoInfo.thumbnailUrl();

            // 2. Content 생성 (확실한 정보 사용)
            Content content = Content.builder()
                    .externalId(externalId)
                    .title(finalTitle) // API 제목
                    .mediaType(MediaType.YOUTUBE)
                    .posterPath(posterPath) // API 썸네일
                    .build();

            // 3. 장르 매핑 (여기서 바로 처리)
            genreRepository.findByCodeAndType(videoInfo.categoryId(), PlatformType.YOUTUBE)
                    .ifPresent(content::addGenre);

            return contentRepository.save(content);

        } catch (Exception e) {
            log.warn("Youtube API Error for video {}: {}", externalId, e.getMessage());

            // API 실패 시: 요청받은 제목 & 기본 썸네일로 생성 (장르 없이)
            Content fallbackContent = Content.builder()
                    .externalId(externalId)
                    .title(title)
                    .mediaType(MediaType.YOUTUBE)
                    .posterPath(posterPath)
                    .build();

            return contentRepository.save(fallbackContent);
        }
    }

    private Content createNetflixContent(String externalId, String originalTitle, TmdbApiClient.TmdbInfo info) {
        String posterPath = (info.getPosterPath() != null && !info.getPosterPath().isEmpty())
                ? info.getPosterPath()
                : s3BaseUrl + DEFAULT_POSTER_PATH;

        Content content = Content.builder()
                .externalId(externalId)
                .tmdbId(info.getTmdbId())
                .title(info.getTitle())
                .originalTitle(originalTitle) // 한글 제목 or 요청 제목
                .mediaType(info.getMediaType())
                .overview(info.getOverview())
                .releaseDate(info.getReleaseDate())
                .posterPath(posterPath)
                .originalLanguage(info.getOriginalLanguage()) // [추가] 언어 저장
                .build();

        // [추가] 장르 저장
        mapGenresToContent(content, info.getGenres(), PlatformType.OTT);

        return contentRepository.save(content);
    }

    /**
     * 이미 존재하는 컨텐츠지만, 상세 정보(장르, 언어 등)를 TMDB 최신으로 동기화
     */
    private void updateOttContentDetails(Content content, Integer tmdbId) {
        if (tmdbId == null)
            return;

        try {
            // API로 상세 정보(장르 ID 목록 포함) 다시 가져오기
            TmdbApiClient.TmdbInfo details = tmdbApiClient.fetchMovieDetails(tmdbId, content.getMediaType());

            // 언어 업데이트
            content.updateOriginalLanguage(details.getOriginalLanguage());

            // 장르 초기화 후 다시 매핑
            content.clearGenres();
            mapGenresToContent(content, details.getGenres(), PlatformType.OTT);
        } catch (Exception e) {
            log.warn("Failed to update OTT content details for tmdbId {}: {}", tmdbId, e.getMessage());
        }
    }

    /**
     * 장르 ID 리스트를 받아 Content 엔티티에 매핑하는 헬퍼 메서드
     */
    private void mapGenresToContent(Content content, List<TmdbApiClient.TmdbInfo.TmdbGenre> genres,
            PlatformType platformType) {
        if (genres == null || genres.isEmpty())
            return;

        for (TmdbApiClient.TmdbInfo.TmdbGenre tmdbGenre : genres) {
            // DB에 있는 장르 코드인 경우만 저장 (없는 코드는 무시 or 에러처리 선택)
            Genre genre = genreRepository.findByCodeAndType(tmdbGenre.id(), platformType)
                    .orElseGet(() -> genreRepository.save(Genre.builder()
                            .code(tmdbGenre.id())
                            .name(tmdbGenre.name())
                            .type(platformType)
                            .build()));

            content.addGenre(genre);
        }
    }
}