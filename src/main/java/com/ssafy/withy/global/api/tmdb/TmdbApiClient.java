package com.ssafy.withy.global.api.tmdb;

import com.ssafy.withy.domain.content.entity.MediaType;
import com.ssafy.withy.global.api.tmdb.dto.TmdbDetailResponse;
import com.ssafy.withy.global.api.tmdb.dto.TmdbResponse;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class TmdbApiClient {

        private final String apiKey;
        private final RestClient restClient;
        private final String baseUrl;

        private final String s3BaseUrl;
        private static final String DEFAULT_POSTER_PATH = "/poster/default_poster.png";
        private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

        @Getter
        @Builder
        public static class TmdbInfo {
                private Integer tmdbId;
                private String title;
                private MediaType mediaType;
                private String posterPath;
                private String overview;
                private String releaseDate;
                private String originalLanguage;
                private List<TmdbGenre> genres;

                public record TmdbGenre(Integer id, String name) {
                }
        }

        // 생성자 주입
        public TmdbApiClient(RestClient.Builder builder,
                        @Value("${tmdb.api-key}") String apiKey,
                        @Value("${tmdb.base-url}") String baseUrl,
                        @Value("${spring.cloud.aws.s3.base-url}") String s3BaseUrl) {
                this.apiKey = apiKey;
                this.baseUrl = baseUrl;
                this.s3BaseUrl = s3BaseUrl;
                this.restClient = builder.baseUrl(baseUrl).build(); // 여기서 빌드!
        }

        public TmdbInfo searchContent(String queryTitle) {

                // 1. URL 만들기 (/search/multi 사용)
                URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search/multi")
                                .queryParam("api_key", apiKey)
                                .queryParam("query", queryTitle)
                                .queryParam("language", "ko-KR") // 한국어 데이터 필수!
                                .queryParam("include_adult", "false")
                                .build()
                                .toUri();

                log.info("TMDB API Request: {}", uri);

                // 2. 요청 보내기
                TmdbResponse response = this.restClient.get()
                                .uri(uri)
                                .retrieve()
                                .body(TmdbResponse.class);

                if (response == null || response.results() == null || response.results().isEmpty()) {
                        log.warn("TMDB 검색 결과 없음: {}", queryTitle);
                        // 검색 결과 없으면 어떻게 할래? 일단 에러 던지거나 기본값?
                        // 여기선 에러 던지고 프론트한테 "야, 제목 똑바로 쓴 거 맞아?" 라고 하는 게 맞음.
                        throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
                }

                // 3. 결과 필터링 (가장 정확한 1개 찾기)
                TmdbResponse.TmdbResultDto bestMatch = response.results().stream()
                                .filter(item -> "movie".equals(item.mediaType()) || "tv".equals(item.mediaType()))
                                .findFirst() // 가장 상단 결과 (정확도 순)
                                .orElseThrow(() -> new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND));

                String fullPosterPath = resolvePosterPath(bestMatch.posterPath());

                // 4. 우리 입맛(DTO)에 맞게 변환
                return TmdbInfo.builder()
                                .tmdbId(bestMatch.id().intValue())
                                .title(bestMatch.title())
                                .mediaType("movie".equals(bestMatch.mediaType()) ? MediaType.MOVIE : MediaType.TV)
                                .posterPath(fullPosterPath) // 전체 URL 완성
                                .overview(bestMatch.overview())
                                .releaseDate(bestMatch.releaseDate())
                                .originalLanguage(bestMatch.originalLanguage())
                                .genres(List.of()) // Search API는 이름 없이 ID만 줘서 빈 리스트 처리 (상세조회에서 채움)
                                .build();
        }

        /**
         * [NEW] 컨텐츠 상세 정보 조회 (장르, 언어 등)
         * TMDB API: GET /movie/{id} OR GET /tv/{id}
         */
        public TmdbInfo fetchMovieDetails(Integer tmdbId, MediaType mediaType) {
                String endpoint = (mediaType == MediaType.MOVIE) ? "/movie/" : "/tv/";

                // 1. 상세 조회 API 호출 (응답 구조가 Search랑 달라서 별도 DTO 필요)
                TmdbDetailResponse response = restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path(endpoint + tmdbId)
                                                .queryParam("api_key", apiKey)
                                                .queryParam("language", "ko-KR") // 한국어 데이터 우선
                                                .build())
                                .retrieve()
                                .body(TmdbDetailResponse.class);

                if (response == null) {
                        throw new CustomException(GlobalErrorCode.TMDB_API_ERROR);
                }

                // 2. TmdbDetailResponse -> TmdbInfo 변환
                List<TmdbInfo.TmdbGenre> tmdbGenres = response.genres().stream()
                                .map(g -> new TmdbInfo.TmdbGenre(g.id(), g.name()))
                                .toList();

                String fullPosterPath = resolvePosterPath(response.posterPath());

                return TmdbInfo.builder()
                                .tmdbId(response.id())
                                .title(response.title() != null ? response.title() : response.name()) // 영화는 title, TV는
                                                                                                      // name
                                .overview(response.overview())
                                .originalLanguage(response.originalLanguage()) // [핵심] 언어
                                .genres(tmdbGenres) // [핵심] 장르 객체 리스트
                                .releaseDate(response.releaseDate() != null ? response.releaseDate()
                                                : response.firstAirDate())
                                .posterPath(fullPosterPath)
                                .mediaType(mediaType) // 요청했던 타입 그대로 유지
                                .build();
        }

        private String resolvePosterPath(String tmdbPosterPath) {
                if (StringUtils.hasText(tmdbPosterPath)) {
                        // TMDB 경로가 정상이면 앞에 도메인 붙여서 리턴
                        return TMDB_IMAGE_BASE_URL + tmdbPosterPath;
                }
                // 없으면 S3 기본 이미지 리턴
                return s3BaseUrl + DEFAULT_POSTER_PATH;
        }
}