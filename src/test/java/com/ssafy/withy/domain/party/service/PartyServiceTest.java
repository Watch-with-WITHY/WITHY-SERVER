package com.ssafy.withy.domain.party.service;

import com.ssafy.withy.domain.content.entity.Content;
import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.content.entity.MediaType;
import com.ssafy.withy.domain.content.service.ContentService;
import com.ssafy.withy.domain.party.dto.*;
import com.ssafy.withy.domain.party.entity.*;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import com.ssafy.withy.domain.user.entity.Subscribe;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ssafy.withy.domain.content.repository.GenreRepository;
import com.ssafy.withy.domain.content.entity.WatchHistory;
import com.ssafy.withy.domain.content.repository.WatchHistoryRepository;
import com.ssafy.withy.domain.party.dto.CategoryPartyResponseDto;
import com.ssafy.withy.domain.user.repository.SubscribeRepository;
import com.ssafy.withy.domain.content.entity.ContentRagContext;
import com.ssafy.withy.domain.content.repository.ContentRagContextRepository;
import com.ssafy.withy.domain.content.client.AiRefinementClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

        @InjectMocks
        private PartyService partyService;

        @Mock
        private PartyRepository partyRepository;

        @Mock
        private GenreRepository genreRepository;

        @Mock
        private SubscribeRepository subscribeRepository;

        @Mock
        private WatchHistoryRepository watchHistoryRepository;

        @Mock
        private com.ssafy.withy.domain.party.repository.ParticipantRepository participantRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private ContentService contentService;

        @Mock
        private PartySessionManager partySessionManager;

        @Mock
        private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

        @Mock
        private com.ssafy.withy.domain.dm.service.DmService dmService;

        @Mock
        private com.ssafy.withy.domain.party.client.AiRecommendationClient aiRecommendationClient;

        @Mock
        private com.ssafy.withy.domain.party.client.AiContextCachingClient aiContextCachingClient;

        @Mock
        private com.ssafy.withy.domain.content.repository.ContentRagContextRepository contentRagContextRepository;

        @Mock
        private com.ssafy.withy.domain.content.client.AiRefinementClient aiRefinementClient;

        @Mock
        private com.ssafy.withy.domain.party.repository.PartyCommandLogRepository partyCommandLogRepository;

        @Test
        @DisplayName("파티 목록 조회 정상 동작")
        void getPartyList_Success() {
                // Given
                Genre genre = Genre.builder().name("Comedy").build();
                Content content = Content.builder().mediaType(MediaType.MOVIE).build();

                // ReflectionTestUtils를 사용하여 contentGenres 설정 (Content의 contentGenres 리스트에 접근)
                // 실제로는 Content 생성 시점에 리스트가 초기화되므로, 테스트 편의상 아래와 같이 Mocking보다는 실제 객체 상태를 만들어주는 것이
                // 좋음
                // 여기서는 간단하게 DTO 변환 로직 테스트를 위해 Party 객체의 상태에 집중

                Party party = Party.builder()
                                .id(1)
                                .title("Test Party")
                                .platform(PlatformType.OTT)
                                .content(content)
                                .currentParticipants(1)
                                .maxParticipants(4)
                                .isActive(true)
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                given(partyRepository.searchParties(eq(PlatformType.OTT), any(), any(), eq(pageable)))
                                .willReturn(new PageImpl<>(List.of(party)));

                // When
                Page<PartyListResponseDto> result = partyService.getPartyList("OTT", "35", null, pageable, null);

                // Then
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).title()).isEqualTo("Test Party");
                assertThat(result.getContent().get(0).host()).isNull(); // 인증되지 않은 사용자
                assertThat(result.getContent().get(0).currentPlaybackTime()).isNull(); // 인증되지 않은 사용자
        }

        @Test
        @DisplayName("통합 검색 정상 동작 - 카테고리 및 파티 검색")
        void searchIntegrated_Success() {
                // Given
                String keyword = "액션";
                Genre genre = Genre.builder().name("액션").code(28).type(PlatformType.OTT).build();
                Party party = Party.builder()
                                .id(1)
                                .title("액션 영화 파티")
                                .platform(PlatformType.OTT)
                                .content(Content.builder().mediaType(MediaType.MOVIE).build())
                                .currentParticipants(1)
                                .maxParticipants(4)
                                .isActive(true)
                                .build();

                Pageable pageable = PageRequest.of(0, 20);
                given(genreRepository.findByNameContaining(keyword)).willReturn(List.of(genre));
                given(partyRepository.findByKeyword(keyword, pageable))
                                .willReturn(new PageImpl<>(List.of(party)));

                // When
                IntegratedSearchResponseDto result = partyService.searchIntegrated(keyword, pageable, null);

                // Then
                assertThat(result.categories()).hasSize(1);
                assertThat(result.categories().get(0).name()).isEqualTo("액션");
                assertThat(result.parties()).hasSize(1);
                assertThat(result.parties().get(0).title()).isEqualTo("액션 영화 파티");
                assertThat(result.parties().get(0).host()).isNull(); // 인증되지 않은 사용자
        }

        @Test
        @DisplayName("팔로우한 카테고리 파티 조회 정상 동작")
        void getFollowedCategoryParties_Success() {
                // Given
                Integer userId = 1;
                Genre genre = Genre.builder().name("액션").code(28).type(PlatformType.OTT).build();
                Subscribe subscribe = Subscribe.builder().genre(genre).build();
                Party party = Party.builder()
                                .id(1)
                                .title("액션 파티")
                                .platform(PlatformType.OTT)
                                .content(Content.builder().mediaType(MediaType.MOVIE).build())
                                .currentParticipants(1)
                                .maxParticipants(4)
                                .isActive(true)
                                .build();

                given(subscribeRepository.findByUser_IdOrderByIdAsc(eq(userId), any(Pageable.class)))
                                .willReturn(List.of(subscribe));
                given(partyRepository.findLatestPartiesByGenre(eq(genre.getId()), any(Pageable.class)))
                                .willReturn(List.of(party));

                // When
                List<CategoryPartyResponseDto> result = partyService.getFollowedCategoryParties(userId);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).genre().name()).isEqualTo("액션");
                assertThat(result.get(0).parties()).hasSize(1);
                assertThat(result.get(0).parties().get(0).title()).isEqualTo("액션 파티");

        }

        @Test
        @DisplayName("인기 카테고리 파티 조회 정상 동작")
        void getPopularCategoryParties_Success() {
                // Given
                Genre genre = Genre.builder().name("인기 장르").code(99).type(PlatformType.OTT).build();
                Party party = Party.builder()
                                .id(1)
                                .title("인기 파티")
                                .platform(PlatformType.OTT)
                                .content(Content.builder().mediaType(MediaType.MOVIE).build())
                                .currentParticipants(10)
                                .maxParticipants(20)
                                .isActive(true)
                                .build();

                given(genreRepository.findTop3PopularGenres(any(Pageable.class))).willReturn(List.of(genre));
                given(partyRepository
                                .findPopularPartiesByGenre(
                                                eq(genre.getId()), any(Pageable.class)))
                                .willReturn(List.of(party));

                // When
                List<CategoryPartyResponseDto> result = partyService.getPopularCategoryParties(null);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).genre().name()).isEqualTo("인기 장르");
                assertThat(result.get(0).parties()).hasSize(1);
                assertThat(result.get(0).parties().get(0).title()).isEqualTo("인기 파티");
                assertThat(result.get(0).parties().get(0).host()).isNull(); // 인증되지 않은 사용자
        }

        @Test
        @DisplayName("이어보기 추천 파티 조회 정상 동작 - 영화 & 시리즈")
        void getContinueWatchingRecommendations_Success() {
                // Given
                Integer userId = 1;
                Content content = Content.builder().id(1).mediaType(MediaType.TV).build();
                WatchHistory watchHistory = WatchHistory.builder()
                                .content(content)
                                .seasonNumber((byte) 1)
                                .episodeNumber(1)
                                .lastPosition(1000) // 1000초 시점
                                .build();

                Party party1 = Party.builder()
                                .id(1)
                                .title("Party 1 (900s ago)")
                                .content(content)
                                .actualActiveTime(LocalDateTime.now().minusSeconds(900)) // 900초 경과
                                .currentParticipants(1)
                                .maxParticipants(4)
                                .isActive(true)
                                .build();

                Party party2 = Party.builder()
                                .id(2)
                                .title("Party 2 (1010s ago)")
                                .content(content)
                                .actualActiveTime(LocalDateTime.now().minusSeconds(1010)) // 1010초 경과 - 가장 가까움 (오차 10초)
                                .currentParticipants(1)
                                .maxParticipants(4)
                                .isActive(true)
                                .build();

                given(watchHistoryRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                                .willReturn(Optional.of(watchHistory));

                // Repository가 actualActiveTime이 null이 아닌 것만 반환한다고 가정
                given(partyRepository.findContinueWatchingParties(1, (byte) 1, 1))
                                .willReturn(List.of(party1, party2));

                // When
                List<PartyListResponseDto> result = partyService.getContinueWatchingRecommendations(userId);

                // Then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).title()).isEqualTo("Party 2 (1010s ago)"); // 오차 10초
                assertThat(result.get(1).title()).isEqualTo("Party 1 (900s ago)"); // 오차 100초
        }

        @Test
        @DisplayName("이어보기 추천 - 시청 기록 없음")
        void getContinueWatchingRecommendations_NoHistory() {
                // Given
                Integer userId = 1;
                given(watchHistoryRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                                .willReturn(Optional.empty());

                // When
                List<PartyListResponseDto> result = partyService.getContinueWatchingRecommendations(userId);

                // Then
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("플랫폼 타입 조회 정상 동작")
        void getPlatformTypes_Success() {
                // When
                List<String> result = partyService.getPlatformTypes();

                // Then
                assertThat(result).containsExactlyInAnyOrder("OTT", "YOUTUBE");
        }

        @Test
        @DisplayName("파티 목록 조회 - 인증된 사용자, 호스트 및 재생 시간 포함")
        void getPartyList_WithAuthenticatedUser_Success() {
                // Given
                Integer userId = 1;
                Genre genre = Genre.builder().name("Comedy").build();
                Content content = Content.builder().id(1).mediaType(MediaType.MOVIE).build();

                com.ssafy.withy.domain.user.entity.User hostUser = com.ssafy.withy.domain.user.entity.User.builder()
                                .id(2)
                                .nickname("HostUser")
                                .profileImageUrl("http://example.com/profile.jpg")
                                .build();

                Party party = Party.builder()
                                .id(1)
                                .title("Test Party")
                                .platform(PlatformType.OTT)
                                .content(content)
                                .currentParticipants(1)
                                .maxParticipants(4)
                                .isActive(true)
                                .build();

                com.ssafy.withy.domain.party.entity.Participant participant = com.ssafy.withy.domain.party.entity.Participant
                                .builder()
                                .party(party)
                                .user(hostUser)
                                .role(com.ssafy.withy.domain.party.entity.ParticipantRole.HOST)
                                .build();

                WatchHistory watchHistory = WatchHistory.builder()
                                .content(content)
                                .lastPosition(1500)
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                given(partyRepository.searchParties(eq(PlatformType.OTT), any(), any(), eq(pageable)))
                                .willReturn(new PageImpl<>(List.of(party)));
                given(participantRepository.findHostsByPartyIds(List.of(1)))
                                .willReturn(List.of(participant));
                given(watchHistoryRepository.findLatestByUserIdAndContentIds(userId, List.of(1)))
                                .willReturn(List.of(watchHistory));

                // When
                Page<PartyListResponseDto> result = partyService.getPartyList("OTT", "35", null, pageable, userId);

                // Then
                assertThat(result.getContent()).hasSize(1);
                PartyListResponseDto dto = result.getContent().get(0);
                assertThat(dto.title()).isEqualTo("Test Party");
                assertThat(dto.host()).isNotNull();
                assertThat(dto.host().userId()).isEqualTo(2);
                assertThat(dto.host().nickname()).isEqualTo("HostUser");
                assertThat(dto.host().profileImageUrl()).isEqualTo("http://example.com/profile.jpg");
                assertThat(dto.currentPlaybackTime()).isEqualTo(1500);
        }

        @Test
        @DisplayName("통합 검색 - 인증된 사용자, 호스트 정보 포함")
        void searchIntegrated_WithAuthenticatedUser_Success() {
                // Given
                Integer userId = 1;
                String keyword = "액션";
                Genre genre = Genre.builder().name("액션").code(28).type(PlatformType.OTT).build();
                Content content = Content.builder().id(1).mediaType(MediaType.MOVIE).build();

                com.ssafy.withy.domain.user.entity.User hostUser = com.ssafy.withy.domain.user.entity.User.builder()
                                .id(2)
                                .nickname("ActionHost")
                                .profileImageUrl("http://example.com/action.jpg")
                                .build();

                Party party = Party.builder()
                                .id(1)
                                .title("액션 영화 파티")
                                .platform(PlatformType.OTT)
                                .content(content)
                                .currentParticipants(1)
                                .maxParticipants(4)
                                .isActive(true)
                                .build();

                com.ssafy.withy.domain.party.entity.Participant participant = com.ssafy.withy.domain.party.entity.Participant
                                .builder()
                                .party(party)
                                .user(hostUser)
                                .role(com.ssafy.withy.domain.party.entity.ParticipantRole.HOST)
                                .build();

                Pageable pageable = PageRequest.of(0, 20);
                given(genreRepository.findByNameContaining(keyword)).willReturn(List.of(genre));
                given(partyRepository.findByKeyword(keyword, pageable))
                                .willReturn(new PageImpl<>(List.of(party)));
                given(participantRepository.findHostsByPartyIds(List.of(1)))
                                .willReturn(List.of(participant));
                given(watchHistoryRepository.findLatestByUserIdAndContentIds(userId, List.of(1)))
                                .willReturn(List.of());

                // When
                IntegratedSearchResponseDto result = partyService.searchIntegrated(keyword, pageable, userId);

                // Then
                assertThat(result.categories()).hasSize(1);
                assertThat(result.parties()).hasSize(1);
                PartyListResponseDto dto = result.parties().get(0);
                assertThat(dto.host()).isNotNull();
                assertThat(dto.host().nickname()).isEqualTo("ActionHost");
                assertThat(dto.currentPlaybackTime()).isNull(); // 시청 기록 없음
        }

        @Test
        @DisplayName("팔로우한 카테고리 파티 조회 - 호스트 정보 포함")
        void getFollowedCategoryParties_WithHostInfo_Success() {
                // Given
                Integer userId = 1;
                Genre genre = Genre.builder().name("액션").code(28).type(PlatformType.OTT).build();
                Subscribe subscribe = Subscribe.builder().genre(genre).build();
                Content content = Content.builder().id(1).mediaType(MediaType.MOVIE).build();

                com.ssafy.withy.domain.user.entity.User hostUser = com.ssafy.withy.domain.user.entity.User.builder()
                                .id(2)
                                .nickname("GenreHost")
                                .profileImageUrl("http://example.com/genre.jpg")
                                .build();

                Party party = Party.builder()
                                .id(1)
                                .title("액션 파티")
                                .platform(PlatformType.OTT)
                                .content(content)
                                .currentParticipants(1)
                                .maxParticipants(4)
                                .isActive(true)
                                .build();

                com.ssafy.withy.domain.party.entity.Participant participant = com.ssafy.withy.domain.party.entity.Participant
                                .builder()
                                .party(party)
                                .user(hostUser)
                                .role(com.ssafy.withy.domain.party.entity.ParticipantRole.HOST)
                                .build();

                given(subscribeRepository.findByUser_IdOrderByIdAsc(eq(userId), any(Pageable.class)))
                                .willReturn(List.of(subscribe));
                given(partyRepository.findLatestPartiesByGenre(eq(genre.getId()), any(Pageable.class)))
                                .willReturn(List.of(party));
                given(participantRepository.findHostsByPartyIds(List.of(1)))
                                .willReturn(List.of(participant));
                given(watchHistoryRepository.findLatestByUserIdAndContentIds(userId, List.of(1)))
                                .willReturn(List.of());

                // When
                List<CategoryPartyResponseDto> result = partyService.getFollowedCategoryParties(userId);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).parties()).hasSize(1);
                PartyListResponseDto dto = result.get(0).parties().get(0);
                assertThat(dto.host()).isNotNull();
                assertThat(dto.host().nickname()).isEqualTo("GenreHost");
        }

        @Test
        @DisplayName("파티 생성 성공 - 넷플릭스(OTT) 예시")
        void createParty_success() {
                // given
                Integer userId = 1;
                User host = User.builder().id(userId).email("host@test.com").build();

                // [변경] DTO가 PlatformType을 받도록 수정됨
                PartyCreateRequest request = new PartyCreateRequest("기묘한 이야기 정주행 파티", // title
                                "80057281", // contentId (externalId)
                                "Stranger Things", // contentTitle
                                PlatformType.OTT, // [변경] MediaType -> PlatformType
                                LocalDateTime.now().plusHours(1),
                                4,
                                false,
                                null);

                // Mock 객체 세팅
                Content mockContent = Content.builder().id(10).title("Stranger Things").build();

                // 저장될 파티 객체 (ID 100L 가정)
                Party savedParty = Party.builder()
                                .id(100) // Integer ID라면 100
                                .host(host)
                                .content(mockContent)
                                .build();

                // Stubbing (가짜 행동 정의)
                given(userRepository.findById(userId)).willReturn(Optional.of(host));

                // [핵심] ContentService의 변경된 메서드 시그니처에 맞춰 Stubbing
                // getOrCreateContent(externalId, title, platformType)
                given(contentService.getOrCreateContent(any(), any(), any())).willReturn(mockContent);

                given(partyRepository.save(any(Party.class))).willReturn(savedParty);

                // [검증] RAG 컨텍스트 확인 로직
                // createParty_success는 기본적으로 RAG가 없는 상황을 가정하거나, 명시적으로 비어있음을 설정
                given(contentRagContextRepository.findById(any())).willReturn(Optional.empty());

                // when
                Integer partyId = partyService.createParty(request, userId);

                // then
                assertNotNull(partyId);
                assertEquals(100, partyId);

                // [검증] ContentService가 올바른 파라미터로 호출되었는지 확인
                then(contentService).should(times(1)).getOrCreateContent(
                                request.contentId(),
                                request.contentTitle(),
                                request.platform() // DTO의 platform 정보가 잘 넘어갔는지 체크
                );

                // [검증] PartyRepository 저장 호출 확인
                then(partyRepository).should(times(1)).save(any(Party.class));

                // [검증] RAG 데이터가 없으므로 정제 요청이 호출되어야 함
                then(aiRefinementClient).should(times(1)).requestRefinement(mockContent);
                // [검증] 캐싱은 호출되지 않아야 함
                then(aiContextCachingClient).should(never()).cacheContext(any(), any());
        }

        @Test
        @DisplayName("파티 생성 성공 - RAG 데이터 존재 시 캐싱 수행")
        void createParty_success_withRAG() {
                // given
                Integer userId = 1;
                User host = User.builder().id(userId).email("host@test.com").build();

                PartyCreateRequest request = new PartyCreateRequest("RAG 파티",
                                "80057281", "Stranger Things", PlatformType.OTT,
                                LocalDateTime.now().plusHours(1), 4, false, null);

                Content mockContent = Content.builder().id(10).title("Stranger Things").build();
                Party savedParty = Party.builder().id(100).host(host).content(mockContent).build();
                
                // RAG 데이터 존재 설정 (Reflection으로 ragText 설정)
                ContentRagContext ragContext = ContentRagContext.builder().build();
                ReflectionTestUtils.setField(ragContext, "ragText", "Detailed AI Summary");

                given(userRepository.findById(userId)).willReturn(Optional.of(host));
                given(contentService.getOrCreateContent(any(), any(), any())).willReturn(mockContent);
                given(partyRepository.save(any(Party.class))).willReturn(savedParty);
                
                // [핵심] RAG 데이터가 있다고 Mocking
                given(contentRagContextRepository.findById(10)).willReturn(Optional.of(ragContext));

                // when
                partyService.createParty(request, userId);

                // then
                // [검증] 캐싱이 ragText로 호출되어야 함
                then(aiContextCachingClient).should(times(1)).cacheContext("100", "Detailed AI Summary");
                // [검증] 정제 요청은 호출되지 않아야 함
                then(aiRefinementClient).should(never()).requestRefinement(any());
        }

        // --- 더미 데이터 생성 헬퍼 ---
        private User createUser(Integer id, String nickname) {
                return User.builder().id(id).nickname(nickname).profileImageUrl("img.jpg").build();
        }

        private Content createContent(Integer id, String title, String externalId) {
                return Content.builder().id(id).title(title).externalId(externalId).tmdbId(id).posterPath("/path")
                                .build();
        }

        private Party createParty(Integer id, User host, Content content) {
                return Party.builder()
                                .id(id)
                                .host(host)
                                .content(content)
                                .title("기본 파티")
                                .maxParticipants(4)
                                .currentParticipants(1)
                                .isPrivate(false)
                                .isActive(false)
                                .platform(PlatformType.OTT)
                                .scheduledActiveTime(LocalDateTime.now().plusHours(1))
                                .build();
        }

        @Nested
        @DisplayName("파티 상세 조회")
        class GetPartyDetail {
                @Test
                @DisplayName("성공: 파티 정보를 DTO로 변환하여 반환한다")
                void success() {
                        // given
                        Integer partyId = 1;
                        User host = createUser(100, "방장");
                        Content content = createContent(200, "오징어게임", "netflix_123");
                        Party party = createParty(partyId, host, content);

                        given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

                        // when
                        PartyDetailResponse response = partyService.getPartyDetail(partyId);

                        // then
                        assertThat(response.partyId()).isEqualTo(partyId);
                        assertThat(response.hostName()).isEqualTo("방장");
                        assertThat(response.contentTitle()).isEqualTo("오징어게임");
                        assertThat(response.tmdbId()).isEqualTo(200); // Integer -> String 변환 확인
                }

                @Test
                @DisplayName("실패: 존재하지 않는 파티면 예외 발생")
                void fail_notFound() {
                        // given
                        given(partyRepository.findById(999)).willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> partyService.getPartyDetail(999))
                                        .isInstanceOf(CustomException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PARTY_NOT_FOUND);
                }
        }

        @Nested
        @DisplayName("파티 정보 수정")
        class UpdateParty {
                @Test
                @DisplayName("성공: 컨텐츠 변경 없이 제목과 인원만 수정")
                void success_basic() {
                        // given
                        Integer partyId = 1;
                        Integer userId = 100;
                        User host = createUser(userId, "방장");
                        Content oldContent = createContent(200, "오징어게임", "netflix_123");
                        Party party = createParty(partyId, host, oldContent);

                        // 컨텐츠 ID가 기존과 동일함
                        PartyUpdateRequest request = new PartyUpdateRequest(
                                        "새로운 제목", 8, true, "1234",
                                        "netflix_123", "오징어게임", PlatformType.OTT);

                        given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

                        // when
                        partyService.updateParty(partyId, userId, request);

                        // then
                        assertThat(party.getTitle()).isEqualTo("새로운 제목");
                        assertThat(party.getMaxParticipants()).isEqualTo(8);
                        assertThat(party.getIsPrivate()).isTrue();
                        assertThat(party.getPassword()).isEqualTo("1234");

                        // ContentService는 호출되지 않아야 함
                        then(contentService).should(never()).getOrCreateContent(any(), any(), any());
                }

                @Test
                @DisplayName("성공: 컨텐츠가 변경되면 ContentService를 통해 갱신한다")
                void success_contentChange() {
                        // given
                        Integer partyId = 1;
                        Integer userId = 100;
                        User host = createUser(userId, "방장");
                        Content oldContent = createContent(200, "오징어게임", "netflix_123");
                        Content newContent = createContent(300, "더 글로리", "netflix_456"); // 새로운 컨텐츠
                        Party party = createParty(partyId, host, oldContent);

                        // 요청에 새로운 컨텐츠 ID("netflix_456")가 들어옴
                        PartyUpdateRequest request = new PartyUpdateRequest(
                                        "제목", 4, false, null,
                                        "netflix_456", "더 글로리", PlatformType.OTT);

                        given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

                        // [핵심] ContentService가 새 컨텐츠를 찾아오도록 Mocking
                        given(contentService.getOrCreateContent("netflix_456", "더 글로리", PlatformType.OTT))
                                        .willReturn(newContent);

                        // when
                        partyService.updateParty(partyId, userId, request);

                        // then
                        assertThat(party.getContent()).isEqualTo(newContent); // 파티의 컨텐츠가 교체됨
                        then(contentService).should().getOrCreateContent("netflix_456", "더 글로리", PlatformType.OTT);
                }

                @Test
                @DisplayName("실패: 방장이 아니면 수정 불가")
                void fail_notHost() {
                        // given
                        Integer partyId = 1;
                        Integer otherUserId = 999; // 방장 아님
                        User host = createUser(100, "방장");
                        Party party = createParty(partyId, host, null);

                        PartyUpdateRequest request = new PartyUpdateRequest("제목", 4, false, null, null, null,
                                        PlatformType.OTT);

                        given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

                        // when & then
                        assertThatThrownBy(() -> partyService.updateParty(partyId, otherUserId, request))
                                        .isInstanceOf(CustomException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PARTY_NOT_PARTY_HOST);
                }
        }

        @Nested
        @DisplayName("파티 활성화")
        class ActivateParty {
                @Test
                @DisplayName("성공: 파티 상태가 Active로 변경되고 시간이 기록된다")
                void success() {
                        // given
                        Integer partyId = 1;
                        Integer userId = 100;
                        User host = createUser(userId, "방장");
                        Party party = createParty(partyId, host, null); // isActive = false 상태

                        given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

                        // when
                        partyService.activateParty(partyId, userId);

                        // then
                        assertThat(party.getIsActive()).isTrue();
                        assertThat(party.getActualActiveTime()).isNotNull();
                }

                @Test
                @DisplayName("실패: 이미 활성화된 파티는 다시 활성화할 수 없다")
                void fail_alreadyActive() {
                        // given
                        Party party = createParty(1, createUser(100, "방장"), null);
                        party.activate(); // 이미 활성화됨

                        given(partyRepository.findById(1)).willReturn(Optional.of(party));

                        // when & then
                        assertThatThrownBy(() -> partyService.activateParty(1, 100))
                                        .isInstanceOf(CustomException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PARTY_ALREADY_ACTIVE);
                }
        }

        // --- Helper Methods (더미 데이터 생성) ---
        private User createUser(Integer id) {
                return User.builder().id(id).build();
        }

        private Party createParty(Integer id, boolean isPrivate, String password, int current, int max) {
                return Party.builder()
                        .id(id)
                        .isPrivate(isPrivate)
                        .password(password)
                        .currentParticipants(current)
                        .maxParticipants(max)
                        .build();
        }

        @Nested
        @DisplayName("파티 입장 (Enter)")
        class EnterPartyTest {

                @Test
                @DisplayName("성공: 모든 조건이 충족되면 입장에 성공한다 (GUEST, JOINED)")
                void enterParty_Success() {
                        // given
                        Integer partyId = 1;
                        Integer userId = 100;
                        // 공개방, 1/5명
                        Party party = createParty(partyId, false, null, 1, 5);
                        User user = createUser(userId);

                        given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(party));
                        given(userRepository.findById(userId)).willReturn(Optional.of(user));

                        // 검증 통과 조건 Mocking
                        given(participantRepository.existsByPartyIdAndUserIdAndStatus(partyId, userId, ParticipantStatus.BANNED))
                                .willReturn(false); // 밴 안 당함
                        given(participantRepository.existsByPartyIdAndUserIdAndStatus(partyId, userId, ParticipantStatus.JOINED))
                                .willReturn(false); // 이미 참여 안 함

                        // when
                        PartyEnterResponse response = partyService.enterParty(partyId, userId, null);

                        // then
                        // 1. Participant가 저장되었는지 확인
                        verify(participantRepository).save(any(Participant.class));
                        // 2. 파티 인원수가 1 -> 2로 증가했는지 확인 (Entity 로직 검증)
                        assertThat(party.getCurrentParticipants()).isEqualTo(2);
                        // 3. 반환값 검증 (DTO 확인)
                        assertThat(response).isNotNull();
                        assertThat(response.getPartyId()).isEqualTo(partyId);
                        assertThat(response.getRole()).isEqualTo(ParticipantRole.GUEST);
                        assertThat(response.getIsHost()).isFalse();
                }

                @Test
                @DisplayName("실패: 강퇴당한 유저(BANNED)는 입장할 수 없다.")
                void enter_Fail_BannedUser() {
                        // given
                        Integer partyId = 1;
                        Integer userId = 100;
                        Party party = createParty(partyId, false, null, 1, 5);
                        User user = createUser(userId);

                        given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(party));
                        given(userRepository.findById(userId)).willReturn(Optional.of(user));

                        // 🚨 BANNED 상태가 true!
                        given(participantRepository.existsByPartyIdAndUserIdAndStatus(partyId, userId, ParticipantStatus.BANNED))
                                .willReturn(true);

                        // when & then
                        assertThatThrownBy(() -> partyService.enterParty(partyId, userId, null))
                                .isInstanceOf(CustomException.class)
                                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PARTY_ALREADY_BANNED_USER);

                        verify(participantRepository, never()).save(any());
                }

                @Test
                @DisplayName("실패: 이미 참여 중인 유저(JOINED)는 중복 입장할 수 없다.")
                void enter_Fail_AlreadyJoined() {
                        // given
                        Integer partyId = 1;
                        Integer userId = 100;
                        Party party = createParty(partyId, false, null, 1, 5);
                        User user = createUser(userId);

                        given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(party));
                        given(userRepository.findById(userId)).willReturn(Optional.of(user));

                        // Banned는 아닌데
                        given(participantRepository.existsByPartyIdAndUserIdAndStatus(partyId, userId, ParticipantStatus.BANNED))
                                .willReturn(false);
                        // 🚨 이미 JOINED 상태임!
                        given(participantRepository.existsByPartyIdAndUserIdAndStatus(partyId, userId, ParticipantStatus.JOINED))
                                .willReturn(true);

                        // when & then
                        assertThatThrownBy(() -> partyService.enterParty(partyId, userId, null))
                                .isInstanceOf(CustomException.class)
                                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PARTY_ALREADY_JOINED_USER);
                }

                @Test
                @DisplayName("실패: 정원이 꽉 찬 파티(FULL)에는 입장할 수 없다.")
                void enter_Fail_PartyFull() {
                        // given
                        Integer partyId = 1;
                        Integer userId = 100;
                        // 5/5 만석
                        Party party = createParty(partyId, false, null, 5, 5);
                        User user = createUser(userId);

                        given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(party));
                        given(userRepository.findById(userId)).willReturn(Optional.of(user));

                        // 다른 조건은 통과
                        given(participantRepository.existsByPartyIdAndUserIdAndStatus(any(), any(), any()))
                                .willReturn(false);

                        // when & then
                        assertThatThrownBy(() -> partyService.enterParty(partyId, userId, null))
                                .isInstanceOf(CustomException.class)
                                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PARTY_FULL);
                }

                @Test
                @DisplayName("실패: 비공개 파티 비밀번호가 틀리면 예외가 발생한다.")
                void enter_Fail_WrongPassword() {
                        // given
                        Integer partyId = 1;
                        Integer userId = 100;
                        // 비밀번호 "1234"
                        Party party = createParty(partyId, true, "1234", 1, 5);
                        User user = createUser(userId);

                        given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(party));
                        given(userRepository.findById(userId)).willReturn(Optional.of(user));
                        given(participantRepository.existsByPartyIdAndUserIdAndStatus(any(), any(), any()))
                                .willReturn(false);

                        // when & then (틀린 비번 "0000" 입력)
                        assertThatThrownBy(() -> partyService.enterParty(partyId, userId, "0000"))
                                .isInstanceOf(CustomException.class)
                                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PARTY_INVALID_PASSWORD);
                }
        }

    @Nested
    @DisplayName("파티 삭제 (Delete)")
    class DeletePartyTest {

        @Test
        @DisplayName("성공: 방장이 요청하면 파티는 Soft Delete 되고, 참가자는 모두 삭제된다.")
        void deleteParty_Success() {
            // given
            Integer partyId = 1;
            Integer hostId = 100;
            User host = createUser(hostId);
            // 방장인 파티 생성
            Content content = createContent(200, "오징어게임", "netflix_123");
            Party party = createParty(partyId, host, content);

            // 락 걸고 조회 Mocking
            given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(party));
            given(userRepository.findById(hostId)).willReturn(Optional.of(host));

            // when
            partyService.deleteParty(partyId, hostId);

            // then
            // 1. 파티 상태 변경 확인 (Soft Delete)
            assertThat(party.getIsDeleted()).isTrue();
            assertThat(party.getIsActive()).isFalse();

            // 2. 참가자 전원 삭제 메서드 호출 확인 (Hard Delete)
            verify(participantRepository).deleteAllByParty(party);
        }
    }

    @Nested
    @DisplayName("파티 퇴장 (Exit)")
    class ExitPartyTest {

        @Test
        @DisplayName("CASE 1 (방장+비활성): 방장이 대기 중에 나가면, DB 삭제 없이 유지된다.")
        void exit_Host_Inactive_Keep() {
            // given
            Integer partyId = 1;
            Integer hostId = 100;
            User host = createUser(hostId);
            Content content = createContent(200, "오징어게임", "netflix_123");
            Party party = createParty(partyId, host, content); // 비활성

            Participant hostParticipant = createParticipant(party, host, ParticipantRole.HOST);

            given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(party));
            given(userRepository.findById(hostId)).willReturn(Optional.of(host));
            given(participantRepository.findByParty_IdAndUser_Id(partyId, hostId))
                    .willReturn(Optional.of(hostParticipant));

            // when
            partyService.exitParty(partyId, hostId);

            // then
            // 1. 아무 일도 안 일어나야 함 (삭제 X, 인원 감소 X)
            verify(participantRepository, never()).delete(any());
            verify(participantRepository, never()).deleteAllByParty(any());

            assertThat(party.getIsDeleted()).isFalse(); // 파티 살아있음
            assertThat(party.getCurrentParticipants()).isEqualTo(1); // 인원수 유지
        }

        @Test
        @DisplayName("CASE 2 (방장+활성): 방장이 진행 중에 나가면, 파티는 삭제된다.")
        void exit_Host_Active_Delete() {
            // given
            Integer partyId = 1;
            Integer hostId = 100;
            User host = createUser(hostId);
            Content content = createContent(200, "오징어게임", "netflix_123");
            Party party = createParty(partyId, host, content);
            party.activate();


            Participant hostParticipant = createParticipant(party, host, ParticipantRole.HOST);

            given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(party));
            given(userRepository.findById(hostId)).willReturn(Optional.of(host));
            given(participantRepository.findByParty_IdAndUser_Id(partyId, hostId))
                    .willReturn(Optional.of(hostParticipant));

            // when
            partyService.exitParty(partyId, hostId);

            // then
            // 1. 파티 삭제 로직이 실행되어야 함
            assertThat(party.getIsDeleted()).isTrue();

            // 2. 참가자 전원 삭제 호출 확인
            verify(participantRepository).deleteAllByParty(party);
        }

        @Test
        @DisplayName("CASE 3 (게스트): 일반 게스트는 정상적으로 퇴장한다.")
        void exit_Guest_Success() {
            // given
            Integer partyId = 1;
            Integer userId = 200;
            User host = createUser(100);
            User guest = createUser(userId);
            Content content = createContent(200, "오징어게임", "netflix_123");
            Party party = createParty(partyId, host, content);
            party.activate();
            // 현재 인원 2명
            setParticipantsCount(party, 2);

            Participant guestParticipant = createParticipant(party, guest, ParticipantRole.GUEST);

            given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(party));
            given(userRepository.findById(userId)).willReturn(Optional.of(guest));
            given(participantRepository.findByParty_IdAndUser_Id(partyId, userId))
                    .willReturn(Optional.of(guestParticipant));

            // when
            partyService.exitParty(partyId, userId);

            // then
            // 1. 해당 게스트만 삭제
            verify(participantRepository).delete(guestParticipant);
            // 2. 인원수 감소 확인 (2 -> 1)
            assertThat(party.getCurrentParticipants()).isEqualTo(1);
        }
    }

    // --- Helper Methods (필요하면 추가) ---
    private Participant createParticipant(Party party, User user, ParticipantRole role) {
        return Participant.builder()
                .party(party)
                .user(user)
                .role(role)
                .status(ParticipantStatus.JOINED)
                .build();
    }

    private void setParticipantsCount(Party party, int count) {
        // (대상 객체, "필드명", 넣을 값)
        ReflectionTestUtils.setField(party, "currentParticipants", count);
    }

        @Nested
        @DisplayName("파티 관리")
        class PartyManagement {

                // --- Helper: 참여자 생성 ---
                private Participant createParticipant(Integer id, Party party, User user, ParticipantRole role) {
                        return Participant.builder()
                                        .id(id)
                                        .party(party)
                                        .user(user)
                                        .role(role)
                                        .status(ParticipantStatus.JOINED)
                                        .build();
                }

                @Test
                @DisplayName("참여자 강퇴 - 매니저가 일반 게스트를 강퇴 (성공)")
                void banParticipant_success_managerBansGuest() {
                        // given
                        Integer partyId = 1;
                        Integer managerId = 200;
                        Integer guestId = 300;

                        Party party = createParty(partyId, createUser(100, "방장"), null);

                        User managerUser = createUser(managerId, "매니저");
                        User guestUser = createUser(guestId, "진상");

                        Participant manager = createParticipant(2, party, managerUser, ParticipantRole.MANAGER);
                        Participant guest = createParticipant(3, party, guestUser, ParticipantRole.GUEST);

                        given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
                        given(participantRepository.findByPartyIdAndUserId(partyId, managerId))
                                        .willReturn(Optional.of(manager));
                        given(participantRepository.findByPartyIdAndUserId(partyId, guestId))
                                        .willReturn(Optional.of(guest));

                        // when
                        partyService.changeParticipantState(partyId, managerId, guestId, ParticipantStatus.BANNED);

                        // then
                        assertThat(guest.getStatus()).isEqualTo(ParticipantStatus.BANNED);
                }

                @Test
                @DisplayName("참여자 강퇴 - 매니저가 방장을 강퇴하려 함 (실패 - 하극상)")
                void banParticipant_fail_mutiny() {
                        // given
                        Integer partyId = 1;
                        Integer managerId = 200;
                        Integer hostId = 100;

                        Party party = createParty(partyId, createUser(hostId, "방장"), null); // 실제 방장

                        Participant manager = createParticipant(2, party, createUser(managerId, "매니저"),
                                        ParticipantRole.MANAGER);
                        Participant host = createParticipant(1, party, createUser(hostId, "방장"), ParticipantRole.HOST); // 참여자로서의
                                                                                                                        // 방장

                        given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
                        given(participantRepository.findByPartyIdAndUserId(partyId, managerId))
                                        .willReturn(Optional.of(manager));
                        given(participantRepository.findByPartyIdAndUserId(partyId, hostId))
                                        .willReturn(Optional.of(host));

                        // when & then
                        assertThatThrownBy(() -> partyService.changeParticipantState(partyId, managerId, hostId,
                                        ParticipantStatus.BANNED))
                                        .isInstanceOf(CustomException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.NOT_ENOUGH_AUTHORITY);
                }

                @Test
                @DisplayName("권한 변경 - 방장 위임 (Host Transfer) 성공")
                void updateRole_delegateHost() {
                        // given
                        Integer partyId = 1;
                        Integer currentHostId = 100;
                        Integer newHostId = 200;

                        User oldHostUser = createUser(currentHostId, "구방장");
                        User newHostUser = createUser(newHostId, "신방장");

                        Party party = createParty(partyId, oldHostUser, null); // 현재 방장은 oldHostUser

                        Participant currentHost = createParticipant(1, party, oldHostUser, ParticipantRole.HOST);
                        Participant targetUser = createParticipant(2, party, newHostUser, ParticipantRole.MANAGER);

                        given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
                        given(participantRepository.findByPartyIdAndUserId(partyId, currentHostId))
                                        .willReturn(Optional.of(currentHost));
                        given(participantRepository.findByPartyIdAndUserId(partyId, newHostId))
                                        .willReturn(Optional.of(targetUser));

                        // when
                        // 구방장이 신방장에게 HOST 권한을 줌
                        partyService.updateParticipantRole(partyId, currentHostId, newHostId, ParticipantRole.HOST);

                        // then
                        // 1. 구방장은 매니저로 강등되었는가?
                        assertThat(currentHost.getRole()).isEqualTo(ParticipantRole.MANAGER);
                        // 2. 신방장은 HOST로 승급되었는가?
                        assertThat(targetUser.getRole()).isEqualTo(ParticipantRole.HOST);
                        // 3. 파티 엔티티의 실제 주인(host 필드)이 바뀌었는가?
                        assertThat(party.getHost()).isEqualTo(newHostUser);
                }
        }


        @Nested
        @DisplayName("내가 만든 파티 목록 조회")
        class MyHostedPartiesTest {
                @Test
                @DisplayName("성공: 내가 호스트인 파티 목록을 페이징하여 조회한다.")
                void getPartiesHostedByMe_Success() {
                        // given
                        Integer userId = 1;
                        User host = createUser(userId);
                        Pageable pageable = PageRequest.of(0, 10);

                        Party party = Party.builder()
                                        .id(1)
                                        .title("My Party")
                                        .host(host)
                                        .platform(PlatformType.OTT)
                                        .currentParticipants(1)
                                        .maxParticipants(4)
                                        .isActive(true)
                                        .isPrivate(false)
                                        .build();

                        Page<Party> partyPage = new PageImpl<>(List.of(party));

                        given(userRepository.findById(userId)).willReturn(Optional.of(host));
                        given(partyRepository.findByHostAndIsDeletedFalse(host, pageable)).willReturn(partyPage);

                        // DTO 변환 시 필요한 Mock
                        // anyList() Stubbing이 불필요하다는 에러 발생 -> Service 내부 동작상 호출되지 않거나,
                        // 테스트 데이터(Content null)로 인해 시청기록 조회는 스킵됨.
                        // Participant 조회 역시 Strict 모드에서 호출 횟수 0이면 에러.
                        // 여기서는 단순히 Repo 호출 결과만 검증하므로 제거하거나 lenient() 사용 가능하지만, 제거가 깔끔함.

                        // when
                        Page<PartyListResponseDto> result = partyService.getPartiesHostedByMe(userId, pageable);

                        // then
                        assertThat(result).isNotNull();
                        assertThat(result.getContent()).hasSize(1);
                        assertThat(result.getContent().get(0).title()).isEqualTo("My Party");
                        verify(partyRepository).findByHostAndIsDeletedFalse(host, pageable);
                }
        }

    @Nested
    @DisplayName("파티 초대 (DM)")
    class InvitationTest {

        @Test
        @DisplayName("성공: 파티 참여자는 친구를 초대할 수 있다")
        void inviteFriend_Success() {
            // given
            Integer partyId = 1;
            Integer senderId = 100;
            Integer targetUserId = 200;
            
            User sender = User.builder().id(senderId).nickname("sender").build();
            Party party = Party.builder()
                    .id(partyId)
                    .title("오징어 게임 시즌2")
                    .platform(PlatformType.OTT)
                    .maxParticipants(5)
                    .currentParticipants(2)
                    .isActive(true)
                    .isPrivate(false)
                    .build();
            
            Participant participant = Participant.builder()
                    .party(party)
                    .user(sender)
                    .role(ParticipantRole.GUEST)
                    .status(ParticipantStatus.JOINED)
                    .build();
            
            PartyInvitationRequest request = new PartyInvitationRequest(targetUserId, "빨리 와!");
            com.ssafy.withy.domain.dm.dto.DmRoomResponse roomResponse = 
                    com.ssafy.withy.domain.dm.dto.DmRoomResponse.builder()
                            .roomId(55)
                            .build();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(participantRepository.findByPartyIdAndUserId(partyId, senderId))
                    .willReturn(Optional.of(participant));
            given(dmService.createRoom(senderId, targetUserId)).willReturn(roomResponse);

            // when
            partyService.inviteFriend(partyId, senderId, request);

            // then
            verify(dmService).createRoom(senderId, targetUserId);
            verify(dmService).sendMessage(eq(senderId), any(com.ssafy.withy.domain.dm.dto.DmMessageRequest.class));
        }

        @Test
        @DisplayName("실패: 파티에 참여하지 않은 유저는 초대할 수 없다")
        void inviteFriend_Fail_NotParticipant() {
            // given
            Integer partyId = 1;
            Integer senderId = 100;
            PartyInvitationRequest request = new PartyInvitationRequest(200, "Hi");

            Party party = Party.builder()
                    .id(partyId)
                    .title("Test Party")
                    .platform(PlatformType.OTT)
                    .maxParticipants(5)
                    .currentParticipants(1)
                    .isActive(false)
                    .isPrivate(false)
                    .build();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(participantRepository.findByPartyIdAndUserId(partyId, senderId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyService.inviteFriend(partyId, senderId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PARTICIPANT_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 파티에는 초대할 수 없다")
        void inviteFriend_Fail_PartyNotFound() {
            // given
            Integer partyId = 999;
            Integer senderId = 100;
            PartyInvitationRequest request = new PartyInvitationRequest(200, "Hi");

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyService.inviteFriend(partyId, senderId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PARTY_NOT_FOUND);
        }
    }
}
