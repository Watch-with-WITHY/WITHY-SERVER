package com.ssafy.withy.domain.content.dto;

import com.ssafy.withy.domain.party.entity.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "시청 기록 저장 요청")
public record WatchHistorySaveRequest(
        @Schema(description = "컨텐츠 외부 ID (TMDB ID or YouTube Video ID)", example = "movie_12345") @NotNull(message = "컨텐츠 식별자는 필수값입니다.") String externalId,
        @Schema(description = "영상 총 길이 (초)", example = "3600") @NotNull(message = "영상 총 길이는 필수값입니다.") @Min(value = 1, message = "영상 총 길이는 1초 이상이어야 합니다.") Integer videoDuration,
        @Schema(description = "마지막 재생 위치 (초)", example = "1500") @NotNull(message = "마지막 재생 위치는 필수값입니다.") @Min(value = 0, message = "마지막 재생 위치는 0 이상이어야 합니다.") Integer lastPosition,
        @Schema(description = "이번 세션 재생 시간 (초)", example = "300") Integer playTimeSeconds,
        @Schema(description = "시청 종료 시간", example = "2024-05-20T10:00:00") LocalDateTime endedAt,
        @Schema(description = "시즌 번호 (시리즈인 경우)", example = "1") Byte seasonNumber,
        @Schema(description = "에피소드 번호 (시리즈인 경우)", example = "1") Integer episodeNumber,
        @Schema(description = "플랫폼 타입 (OTT, YOUTUBE)", example = "OTT") PlatformType platform) {
}
