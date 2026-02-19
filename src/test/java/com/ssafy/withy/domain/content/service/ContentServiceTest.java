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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

        @Mock
        private ContentRepository contentRepository;
        @Mock
        private GenreRepository genreRepository;
        @Mock
        private TmdbApiClient tmdbApiClient;
        @Mock
        private YoutubeApiClient youtubeApiClient;

        @InjectMocks
        private ContentService contentService;

        @Test
        @DisplayName("유튜브 컨텐츠 생성 시 API 정보를 활용하여 저장한다")
        void createYoutubeContent_success() {
                // given
                String videoId = "video123";
                String inputTitle = "입력된 제목"; // 이건 무시되고 API 제목이 들어가야 함

                // API가 리턴할 가짜 정보 (Record 생성)
                String apiTitle = "API에서 가져온 진짜 제목";
                String apiThumbnail = "https://i.ytimg.com/vi/video123/maxresdefault.jpg";
                Integer genreCode = 24; // 엔터테인먼트

                YoutubeVideoInfo mockInfo = new YoutubeVideoInfo(apiTitle, apiThumbnail, genreCode);
                Genre genre = Genre.builder().id(1).code(genreCode).name("엔터테인먼트").build();

                // Mocking
                given(contentRepository.findByExternalId(videoId)).willReturn(Optional.empty());

                // [핵심 변경] fetchVideoCategory -> fetchVideoInfo, 리턴값은 mockInfo 객체!
                given(youtubeApiClient.fetchVideoInfo(videoId)).willReturn(mockInfo);

                given(genreRepository.findByCodeAndType(genreCode, PlatformType.YOUTUBE))
                                .willReturn(Optional.of(genre));
                given(contentRepository.save(any(Content.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                Content result = contentService.getOrCreateContent(videoId, inputTitle, PlatformType.YOUTUBE);

                // then
                assertThat(result.getExternalId()).isEqualTo(videoId);

                // [검증] 입력된 제목이 아니라 API 제목과 썸네일이 들어갔는지 확인!
                assertThat(result.getTitle()).isEqualTo(apiTitle);
                assertThat(result.getPosterPath()).isEqualTo(apiThumbnail);

                // 장르 확인
                assertThat(result.getContentGenres()).hasSize(1);
                assertThat(result.getContentGenres().get(0).getGenre().getName()).isEqualTo("엔터테인먼트");

                // 메서드 호출 검증 변경
                then(youtubeApiClient).should().fetchVideoInfo(videoId);
        }

        @Test
        @DisplayName("OTT 컨텐츠가 이미 존재하더라도, TMDB 상세 정보를 통해 장르와 언어를 갱신한다")
        void updateOttContent_success() {
                // given
                String netflixId = "888888";
                Integer tmdbId = 12345;
                String title = "오징어 게임";

                // 기존에 DB에 있던 컨텐츠 (장르가 없다고 가정)
                Content existingContent = Content.builder()
                                .id(1)
                                .externalId(netflixId)
                                .tmdbId(tmdbId)
                                .title(title)
                                .mediaType(MediaType.TV)
                                .originalLanguage("en") // 예전엔 영어로 잘못 되어있었다고 가정
                                .build();

                // TMDB API가 돌려줄 최신 상세 정보
                TmdbApiClient.TmdbInfo tmdbDetails = TmdbApiClient.TmdbInfo.builder()
                                .tmdbId(tmdbId)
                                .title(title)
                                .originalLanguage("ko") // [갱신 포인트 1] 한국어로 변경됨
                                .genres(List.of(
                                                new TmdbApiClient.TmdbInfo.TmdbGenre(18, "드라마"),
                                                new TmdbApiClient.TmdbInfo.TmdbGenre(9648, "미스터리"))) // [갱신 포인트 2]
                                                                                                     // 드라마(18),
                                                                                                     // 미스터리(9648)
                                .build();

                Genre drama = Genre.builder().code(18).name("드라마").build();
                Genre mystery = Genre.builder().code(9648).name("미스터리").build();

                // Mocking
                given(contentRepository.findByExternalId(netflixId)).willReturn(Optional.of(existingContent)); // DB에
                                                                                                               // 있음!

                // [핵심] 갱신 로직(updateOttContentDetails) 내부에서 API 호출
                given(tmdbApiClient.fetchMovieDetails(tmdbId, MediaType.TV)).willReturn(tmdbDetails);

                // 장르 조회 Mocking
                given(genreRepository.findByCodeAndType(18, PlatformType.OTT)).willReturn(Optional.of(drama));
                given(genreRepository.findByCodeAndType(9648, PlatformType.OTT)).willReturn(Optional.of(mystery));

                // when
                Content result = contentService.getOrCreateContent(netflixId, title, PlatformType.OTT);

                // then
                // 1. 객체는 기존 객체여야 함
                assertThat(result).isEqualTo(existingContent);

                // 2. 언어가 'ko'로 갱신되었는지 확인
                assertThat(result.getOriginalLanguage()).isEqualTo("ko");

                // 3. 장르가 2개로 갱신되었는지 확인
                assertThat(result.getContentGenres()).hasSize(2);
                assertThat(result.getContentGenres())
                                .extracting(cg -> cg.getGenre().getName())
                                .containsExactlyInAnyOrder("드라마", "미스터리");

                // 4. 호출 확인
                then(tmdbApiClient).should().fetchMovieDetails(tmdbId, MediaType.TV);
        }
}