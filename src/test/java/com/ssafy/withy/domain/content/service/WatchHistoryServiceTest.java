package com.ssafy.withy.domain.content.service;

import com.ssafy.withy.domain.content.dto.WatchHistoryResponse;
import com.ssafy.withy.domain.content.dto.WatchHistorySaveRequest;
import com.ssafy.withy.domain.content.dto.WatchHistorySaveResponse;
import com.ssafy.withy.domain.content.entity.Content;
import com.ssafy.withy.domain.content.entity.MediaType;
import com.ssafy.withy.domain.content.entity.WatchHistory;
import com.ssafy.withy.domain.content.repository.ContentRepository;
import com.ssafy.withy.domain.content.repository.WatchHistoryRepository;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class WatchHistoryServiceTest {

        @Mock
        private WatchHistoryRepository watchHistoryRepository;
        @Mock
        private ContentRepository contentRepository;
        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private WatchHistoryService watchHistoryService;

        @Test
        @DisplayName("신규 시청 기록 저장 - 컨텐츠 처음 시청")
        void saveOrUpdateHistory_create_success() {
                // given
                Integer userId = 1;
                Integer contentId = 100;
                String externalId = "movie_12345";

                User user = User.builder().id(userId).email("test@test.com").build();
                Content content = Content.builder().id(contentId).externalId(externalId).title("오징어 게임")
                                .mediaType(MediaType.TV).build();

                WatchHistorySaveRequest request = new WatchHistorySaveRequest(
                                externalId, 3600, 1500, 300, null, null, null,
                                com.ssafy.withy.domain.party.entity.PlatformType.OTT);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(contentRepository.findByExternalId(externalId)).willReturn(Optional.of(content));
                given(watchHistoryRepository.findByUserIdAndContentId(userId, contentId)).willReturn(Optional.empty());

                WatchHistory savedHistory = WatchHistory.builder()
                                .id(999)
                                .user(user)
                                .content(content)
                                .duration(3600)
                                .lastPosition(1500)
                                .playTimeSeconds(300)
                                .startedAt(LocalDateTime.now())
                                .build();

                given(watchHistoryRepository.save(any(WatchHistory.class))).willReturn(savedHistory);

                // when
                WatchHistorySaveResponse response = watchHistoryService.saveOrUpdateHistory(userId, request);

                // then
                assertThat(response.id()).isEqualTo(999);
                then(watchHistoryRepository).should().save(any(WatchHistory.class));
        }

        @Test
        @DisplayName("기존 시청 기록 업데이트 - 이어보기")
        void saveOrUpdateHistory_update_success() {
                // given
                Integer userId = 1;
                Integer contentId = 100;
                String externalId = "movie_12345";

                User user = User.builder().id(userId).email("test@test.com").build();
                Content content = Content.builder().id(contentId).externalId(externalId).title("오징어 게임").build();

                // 기존 시청 기록 (lastPosition = 1000)
                WatchHistory existingHistory = WatchHistory.builder()
                                .id(500)
                                .user(user)
                                .content(content)
                                .duration(3600)
                                .lastPosition(1000)
                                .startedAt(LocalDateTime.now().minusDays(1))
                                .createdAt(LocalDateTime.now().minusDays(1))
                                .build();

                // 새로운 요청 (lastPosition = 2000으로 업데이트)
                WatchHistorySaveRequest request = new WatchHistorySaveRequest(
                                externalId, 3600, 2000, 500, null, null, null,
                                com.ssafy.withy.domain.party.entity.PlatformType.OTT);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(contentRepository.findByExternalId(externalId)).willReturn(Optional.of(content));
                given(watchHistoryRepository.findByUserIdAndContentId(userId, contentId))
                                .willReturn(Optional.of(existingHistory));

                // 업데이트된 엔티티
                WatchHistory updatedHistory = WatchHistory.builder()
                                .id(500) // 기존 ID 유지
                                .user(user)
                                .content(content)
                                .duration(3600)
                                .lastPosition(2000)
                                .playTimeSeconds(500)
                                .startedAt(existingHistory.getStartedAt())
                                .createdAt(existingHistory.getCreatedAt())
                                .build();

                given(watchHistoryRepository.save(any(WatchHistory.class))).willReturn(updatedHistory);

                // when
                WatchHistorySaveResponse response = watchHistoryService.saveOrUpdateHistory(userId, request);

                // then
                assertThat(response.id()).isEqualTo(500); // ID는 그대로 유지

                ArgumentCaptor<WatchHistory> captor = ArgumentCaptor.forClass(WatchHistory.class);
                then(watchHistoryRepository).should().save(captor.capture());

                WatchHistory capturedHistory = captor.getValue();
                assertThat(capturedHistory.getId()).isEqualTo(500);
                assertThat(capturedHistory.getLastPosition()).isEqualTo(2000);
        }

        @Test
        @DisplayName("시청 기록 저장 실패 - 존재하지 않는 사용자")
        void saveOrUpdateHistory_userNotFound() {
                // given
                Integer userId = 999;
                WatchHistorySaveRequest request = new WatchHistorySaveRequest("movie_123", 3600, 1500, null, null, null,
                                null,
                                null);

                given(userRepository.findById(userId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> watchHistoryService.saveOrUpdateHistory(userId, request))
                                .isInstanceOf(CustomException.class)
                                .hasMessageContaining("존재하지 않는 회원입니다");
        }

        @Test
        @DisplayName("시청 기록 저장 실패 - 존재하지 않는 컨텐츠")
        void saveOrUpdateHistory_contentNotFound() {
                // given
                Integer userId = 1;
                String externalId = "movie_9999";

                User user = User.builder().id(userId).email("test@test.com").build();
                WatchHistorySaveRequest request = new WatchHistorySaveRequest(externalId, 3600, 1500, null, null, null,
                                null, null);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(contentRepository.findByExternalId(externalId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> watchHistoryService.saveOrUpdateHistory(userId, request))
                                .isInstanceOf(CustomException.class)
                                .hasMessageContaining("존재하지 않는 컨텐츠입니다");
        }

        @Test
        @DisplayName("시청 기록 조회 - 진행률 계산 확인")
        void getMyWatchHistories_progressCalculation() {
                // given
                Integer userId = 1;
                Pageable pageable = PageRequest.of(0, 20);

                User user = User.builder().id(userId).build();
                Content content1 = Content.builder().id(100).title("오징어 게임").posterPath("/poster1.jpg").build();
                Content content2 = Content.builder().id(101).title("지옥").posterPath("/poster2.jpg").build();

                WatchHistory history1 = WatchHistory.builder()
                                .id(1)
                                .user(user)
                                .content(content1)
                                .duration(3600)
                                .lastPosition(1800) // 50% 진행
                                .build();

                WatchHistory history2 = WatchHistory.builder()
                                .id(2)
                                .user(user)
                                .content(content2)
                                .duration(7200)
                                .lastPosition(2400) // 33.33% 진행
                                .build();

                given(watchHistoryRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable))
                                .willReturn(List.of(history1, history2));

                // when
                List<WatchHistoryResponse> responses = watchHistoryService.getMyWatchHistories(userId, pageable);

                // then
                assertThat(responses).hasSize(2);

                // 첫 번째 시청 기록 검증
                WatchHistoryResponse response1 = responses.get(0);
                assertThat(response1.contentId()).isEqualTo(100);
                assertThat(response1.title()).isEqualTo("오징어 게임");
                assertThat(response1.progress()).isEqualTo(0.5); // 1800 / 3600

                // 두 번째 시청 기록 검증
                WatchHistoryResponse response2 = responses.get(1);
                assertThat(response2.contentId()).isEqualTo(101);
                assertThat(response2.progress()).isCloseTo(0.333, org.assertj.core.data.Offset.offset(0.01)); // 2400 /
                                                                                                              // 7200
        }

        @Test
        @DisplayName("시청 기록 조회 - 빈 리스트")
        void getMyWatchHistories_emptyList() {
                // given
                Integer userId = 1;
                Pageable pageable = PageRequest.of(0, 20);

                given(watchHistoryRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable))
                                .willReturn(List.of());

                // when
                List<WatchHistoryResponse> responses = watchHistoryService.getMyWatchHistories(userId, pageable);

                // then
                assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("시청 기록 조회 - AI 연동용 플랫폼 매핑 확인 (YOUTUBE vs OTT)")
        void getMyWatchHistories_platformMapping() {
                // given
                Integer userId = 1;
                Pageable pageable = PageRequest.of(0, 20);

                User user = User.builder().id(userId).build();

                // Case 1: YOUTUBE Content
                Content youtubeContent = Content.builder()
                        .id(101)
                        .title("YouTube Video")
                        .mediaType(MediaType.YOUTUBE)
                        .build();

                // Case 2: MOVIE (Netflix etc) -> Should be OTT
                Content movieContent = Content.builder()
                        .id(102)
                        .title("Netflix Movie")
                        .mediaType(MediaType.MOVIE)
                        .build();

                WatchHistory history1 = WatchHistory.builder().id(1).user(user).content(youtubeContent).duration(100).lastPosition(50).build();
                WatchHistory history2 = WatchHistory.builder().id(2).user(user).content(movieContent).duration(100).lastPosition(50).build();

                given(watchHistoryRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable))
                        .willReturn(List.of(history1, history2));

                // when
                List<WatchHistoryResponse> responses = watchHistoryService.getMyWatchHistories(userId, pageable);

                // then
                assertThat(responses).hasSize(2);

                // Check Case 1
                assertThat(responses.get(0).platform()).isEqualTo(com.ssafy.withy.domain.party.entity.PlatformType.YOUTUBE);

                // Check Case 2
                assertThat(responses.get(1).platform()).isEqualTo(com.ssafy.withy.domain.party.entity.PlatformType.OTT);
        }
}
