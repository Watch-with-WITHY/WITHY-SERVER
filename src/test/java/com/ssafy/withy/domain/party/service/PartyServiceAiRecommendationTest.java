package com.ssafy.withy.domain.party.service;

import com.ssafy.withy.domain.content.entity.Content;
import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.user.entity.Subscribe;
import com.ssafy.withy.domain.user.repository.SubscribeRepository;
import com.ssafy.withy.domain.party.client.AiRecommendationClient;
import com.ssafy.withy.domain.party.dto.PartyListResponseDto;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import com.ssafy.withy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * AI 추천 파티 조회 기능 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class PartyServiceAiRecommendationTest {

        @Mock
        private PartyRepository partyRepository;

        @Mock
        private SubscribeRepository subscribeRepository;

        @Mock
        private AiRecommendationClient aiRecommendationClient;

        @Mock
        private com.ssafy.withy.domain.party.repository.ParticipantRepository participantRepository;

        @Mock
        private com.ssafy.withy.domain.content.repository.WatchHistoryRepository watchHistoryRepository;

        @Mock
        private com.ssafy.withy.domain.content.repository.GenreRepository genreRepository;

        @Mock
        private com.ssafy.withy.domain.user.repository.UserRepository userRepository;

        @Mock
        private com.ssafy.withy.domain.content.service.ContentService contentService;

        @Mock
        private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

        @Mock
        private com.ssafy.withy.domain.party.service.PartySessionManager partySessionManager;

        @Mock
        private com.ssafy.withy.domain.dm.service.DmService dmService;

        @Mock
        private com.ssafy.withy.domain.party.client.AiContextCachingClient aiContextCachingClient;

        @Mock
        private com.ssafy.withy.domain.content.repository.ContentRagContextRepository contentRagContextRepository;

        @Mock
        private com.ssafy.withy.domain.content.client.AiRefinementClient aiRefinementClient;

        @InjectMocks
        private PartyService partyService;

        @Nested
        @DisplayName("AI 추천 파티 조회")
        class GetRecommendedPartiesTest {

                @Test
                @DisplayName("성공: AI 서버가 추천한 영화에 해당하는 활성 파티를 반환한다")
                void getRecommendedParties_Success() {
                        // given
                        Integer userId = 1;
                        Integer topK = 4;

                        // 사용자 선호 장르
                        Genre actionGenre = Genre.builder().id(28).name("Action").build();
                        Genre sciFiGenre = Genre.builder().id(878).name("Sci-Fi").build();
                        Subscribe subscribe1 = Subscribe.builder().genre(actionGenre).build();
                        Subscribe subscribe2 = Subscribe.builder().genre(sciFiGenre).build();
                        given(subscribeRepository.findAllByUser_Id(userId))
                                        .willReturn(List.of(subscribe1, subscribe2));

                        // 활성 파티 목록
                        Content content1 = createContent(1, 550, "Fight Club");
                        Content content2 = createContent(2, 999, "Inception");
                        Party party1 = createParty(1, content1, 3, 5, false);
                        Party party2 = createParty(2, content2, 2, 4, false);
                        given(partyRepository.findAllByIsDeletedFalse())
                                        .willReturn(List.of(party1, party2));

                        // AI 서버 추천 결과
                        List<Integer> recommendedMovieIds = List.of(550, 999);
                        given(aiRecommendationClient.getRecommendations(eq(userId), anyList(), anyList(), eq(topK)))
                                        .willReturn(recommendedMovieIds);

                        // 추천 영화에 해당하는 파티
                        given(partyRepository.findAllByContent_TmdbIdAndIsDeletedFalse(550))
                                        .willReturn(List.of(party1));
                        given(partyRepository.findAllByContent_TmdbIdAndIsDeletedFalse(999))
                                        .willReturn(List.of(party2));

                        // when
                        List<PartyListResponseDto> result = partyService.getRecommendedParties(userId, topK);

                        // then
                        assertThat(result).hasSize(2);
                        assertThat(result.get(0).id()).isEqualTo(1);
                        assertThat(result.get(1).id()).isEqualTo(2);
                }

                @Test
                @DisplayName("Fallback: AI 서버 장애 시 최근 생성된 인기 파티를 반환한다")
                void getRecommendedParties_Fallback_WhenAiServerFails() {
                        // given
                        Integer userId = 1;
                        Integer topK = 4;

                        // 사용자 선호 장르
                        Genre actionGenre = Genre.builder().id(28).name("Action").build();
                        Subscribe subscribe = Subscribe.builder().genre(actionGenre).build();
                        given(subscribeRepository.findAllByUser_Id(userId))
                                        .willReturn(List.of(subscribe));

                        // 활성 파티 목록
                        Content content1 = createContent(1, 550, "Fight Club");
                        Party party1 = createParty(1, content1, 3, 5, false);
                        given(partyRepository.findAllByIsDeletedFalse())
                                        .willReturn(List.of(party1));

                        // AI 서버 장애 (빈 리스트 반환)
                        given(aiRecommendationClient.getRecommendations(eq(userId), anyList(), anyList(), eq(topK)))
                                        .willReturn(List.of());

                        // Fallback: 최근 생성된 인기 파티
                        Party fallbackParty = createParty(2, content1, 5, 5, false);
                        given(partyRepository.findAllByCreatedAtAfterAndIsDeletedFalse(
                                        any(LocalDateTime.class)))
                                        .willReturn(List.of(fallbackParty));

                        // when
                        List<PartyListResponseDto> result = partyService.getRecommendedParties(userId, topK);

                        // then
                        assertThat(result).hasSize(1);
                        assertThat(result.get(0).id()).isEqualTo(2);
                }

                @Test
                @DisplayName("엣지 케이스: 추천 영화에 활성 파티가 없으면 해당 영화는 제외된다")
                void getRecommendedParties_ExcludesMoviesWithoutActiveParties() {
                        // given
                        Integer userId = 1;
                        Integer topK = 4;

                        // 사용자 선호 장르
                        Genre actionGenre = Genre.builder().id(28).name("Action").build();
                        Subscribe subscribe = Subscribe.builder().genre(actionGenre).build();
                        given(subscribeRepository.findAllByUser_Id(userId))
                                        .willReturn(List.of(subscribe));

                        // 활성 파티 목록
                        Content content1 = createContent(1, 550, "Fight Club");
                        Party party1 = createParty(1, content1, 3, 5, false);
                        given(partyRepository.findAllByIsDeletedFalse())
                                        .willReturn(List.of(party1));

                        // AI 서버 추천 결과 (2개 영화)
                        List<Integer> recommendedMovieIds = List.of(550, 999);
                        given(aiRecommendationClient.getRecommendations(eq(userId), anyList(), anyList(), eq(topK)))
                                        .willReturn(recommendedMovieIds);

                        // 첫 번째 영화만 활성 파티 존재
                        given(partyRepository.findAllByContent_TmdbIdAndIsDeletedFalse(550))
                                        .willReturn(List.of(party1));
                        given(partyRepository.findAllByContent_TmdbIdAndIsDeletedFalse(999))
                                        .willReturn(List.of()); // 활성 파티 없음

                        // when
                        List<PartyListResponseDto> result = partyService.getRecommendedParties(userId, topK);

                        // then
                        assertThat(result).hasSize(1); // 활성 파티가 있는 영화만 반환
                        assertThat(result.get(0).id()).isEqualTo(1);
                }

                @Test
                @DisplayName("엣지 케이스: 비공개 파티는 추천에서 제외된다")
                void getRecommendedParties_ExcludesPrivateParties() {
                        // given
                        Integer userId = 1;
                        Integer topK = 4;

                        // 사용자 선호 장르
                        Genre actionGenre = Genre.builder().id(28).name("Action").build();
                        Subscribe subscribe = Subscribe.builder().genre(actionGenre).build();
                        given(subscribeRepository.findAllByUser_Id(userId))
                                        .willReturn(List.of(subscribe));

                        // 활성 파티 목록
                        Content content1 = createContent(1, 550, "Fight Club");
                        Party publicParty = createParty(1, content1, 3, 5, false); // 공개
                        Party privateParty = createParty(2, content1, 4, 5, true); // 비공개
                        given(partyRepository.findAllByIsDeletedFalse())
                                        .willReturn(List.of(publicParty, privateParty));

                        // AI 서버 추천 결과
                        List<Integer> recommendedMovieIds = List.of(550);
                        given(aiRecommendationClient.getRecommendations(eq(userId), anyList(), anyList(), eq(topK)))
                                        .willReturn(recommendedMovieIds);

                        // 공개/비공개 파티 모두 존재
                        given(partyRepository.findAllByContent_TmdbIdAndIsDeletedFalse(550))
                                        .willReturn(List.of(publicParty, privateParty));

                        // when
                        List<PartyListResponseDto> result = partyService.getRecommendedParties(userId, topK);

                        // then
                        assertThat(result).hasSize(1); // 공개 파티만 반환
                        assertThat(result.get(0).id()).isEqualTo(1);
                }
        }

        // Helper methods
        private Content createContent(Integer id, Integer tmdbId, String title) {
                return Content.builder()
                                .id(id)
                                .tmdbId(tmdbId)
                                .title(title)
                                .build();
        }

        private Party createParty(Integer id, Content content, int current, int max, boolean isPrivate) {
                User host = User.builder().id(1).nickname("host").build();
                Party party = Party.builder()
                                .id(id)
                                .host(host)
                                .content(content)
                                .title(content.getTitle() + " 파티")
                                .currentParticipants(current)
                                .maxParticipants(max)
                                .isPrivate(isPrivate)
                                .isActive(true)
                                .build();

                try {
                        java.lang.reflect.Field createdAtField = party.getClass().getSuperclass()
                                        .getDeclaredField("createdAt");
                        createdAtField.setAccessible(true);
                        createdAtField.set(party, LocalDateTime.now());
                } catch (Exception e) {
                        throw new RuntimeException("Failed to set createdAt", e);
                }

                return party;
        }
}
