package com.ssafy.withy.domain.content.service;

import com.ssafy.withy.domain.content.dto.WatchHistoryResponse;
import com.ssafy.withy.domain.content.dto.WatchHistorySaveRequest;
import com.ssafy.withy.domain.content.dto.WatchHistorySaveResponse;
import com.ssafy.withy.domain.content.entity.Content;
import com.ssafy.withy.domain.party.entity.PlatformType;
import com.ssafy.withy.domain.content.entity.WatchHistory;
import com.ssafy.withy.domain.content.repository.ContentRepository;
import com.ssafy.withy.domain.content.repository.WatchHistoryRepository;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchHistoryService {

        private final WatchHistoryRepository watchHistoryRepository;
        private final ContentRepository contentRepository;
        private final UserRepository userRepository;

        /**
         * 시청 기록 저장 또는 갱신
         * - 이미 동일 사용자+컨텐츠 조합이 있으면 업데이트
         * - 없으면 새로 생성
         */
        @Transactional
        public WatchHistorySaveResponse saveOrUpdateHistory(Integer userId, WatchHistorySaveRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

                Content content = contentRepository.findByExternalId(request.externalId())
                                .orElseThrow(() -> new CustomException(GlobalErrorCode.CONTENT_NOT_FOUND));

                // 기존 시청 기록 조회
                WatchHistory watchHistory = watchHistoryRepository
                                .findByUserIdAndContentId(userId, content.getId())
                                .orElse(null);

                if (watchHistory != null) {
                        // 업데이트
                        watchHistory = WatchHistory.builder()
                                        .id(watchHistory.getId())
                                        .user(user)
                                        .content(content)
                                        .duration(request.videoDuration()) // WatchHistory 엔티티의 필드명은 duration
                                        .lastPosition(request.lastPosition())
                                        .playTimeSeconds(request.playTimeSeconds())
                                        .endedAt(request.endedAt())
                                        .seasonNumber(request.seasonNumber())
                                        .episodeNumber(request.episodeNumber())
                                        .startedAt(watchHistory.getStartedAt())
                                        .createdAt(watchHistory.getCreatedAt())
                                        .build();
                } else {
                        // 신규 생성
                        watchHistory = WatchHistory.builder()
                                        .user(user)
                                        .content(content)
                                        .duration(request.videoDuration())
                                        .lastPosition(request.lastPosition())
                                        .playTimeSeconds(request.playTimeSeconds())
                                        .endedAt(request.endedAt())
                                        .startedAt(LocalDateTime.now())
                                        .seasonNumber(request.seasonNumber())
                                        .episodeNumber(request.episodeNumber())
                                        .build();
                }

                WatchHistory saved = watchHistoryRepository.save(watchHistory);
                return new WatchHistorySaveResponse(saved.getId());
        }

        /**
         * 사용자의 시청 기록 목록 조회
         */
        @Transactional(readOnly = true)
        public List<WatchHistoryResponse> getMyWatchHistories(Integer userId, Pageable pageable) {
                List<WatchHistory> histories = watchHistoryRepository.findByUserIdOrderByUpdatedAtDesc(userId,
                                pageable);

                return histories.stream()
                                .map(this::toWatchHistoryResponse)
                                .collect(Collectors.toList());
        }

        /**
         * AI 전용 시청 기록 조회 (간소화된 응답, YOUTUBE 제외)
         */
        @Transactional(readOnly = true)
        public List<com.ssafy.withy.domain.content.dto.AiWatchHistoryResponse> getMyWatchHistoriesForAi(Integer userId, Pageable pageable) {
                // AI 학습용 데이터에서는 YouTube 시청 기록을 제외하고 OTT 데이터만 제공
                List<WatchHistory> histories = watchHistoryRepository.findByUserIdAndContent_MediaTypeNotOrderByUpdatedAtDesc(
                        userId,
                        com.ssafy.withy.domain.content.entity.MediaType.YOUTUBE,
                        pageable);

                return histories.stream()
                        .map(history -> new com.ssafy.withy.domain.content.dto.AiWatchHistoryResponse(
                                history.getContent().getId(),
                                history.getEndedAt()
                        ))
                        .collect(Collectors.toList());
        }

        /**
         * WatchHistory -> WatchHistoryResponse 변환
         */
        private WatchHistoryResponse toWatchHistoryResponse(WatchHistory history) {
                // 진행률 계산
                double progress = history.getDuration() > 0
                                ? (double) history.getLastPosition() / history.getDuration()
                                : 0.0;

                // PlatformType 매핑
                PlatformType platform = (history.getContent()
                                .getMediaType() == com.ssafy.withy.domain.content.entity.MediaType.YOUTUBE)
                                                ? PlatformType.YOUTUBE
                                                : PlatformType.OTT;

                return new WatchHistoryResponse(
                                history.getId(),
                                history.getContent().getId(),
                                history.getContent().getTitle(),
                                history.getLastPosition(),
                                history.getDuration(),
                                progress,
                                history.getContent().getPosterPath(),
                                history.getEndedAt(),
                                platform);
        }
}
