package com.ssafy.withy.domain.content.dto;

import com.ssafy.withy.domain.party.entity.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시청 기록 조회 응답")
public record WatchHistoryResponse(
                @Schema(description = "시청 기록 ID", example = "456") Integer id,

                @Schema(description = "컨텐츠 ID", example = "123") Integer contentId,

                @Schema(description = "컨텐츠 제목", example = "오징어 게임") String title,

                @Schema(description = "마지막 재생 위치 (초)", example = "1500") Integer lastPosition,

                @Schema(description = "영상 총 길이 (초)", example = "3600") Integer videoDuration,

                @Schema(description = "진행률 (0.0 ~ 1.0)", example = "0.416") Double progress,

                @Schema(description = "썸네일/포스터 이미지 경로", example = "/path/to/image.jpg") String thumbnailPath,
                
                @Schema(description = "시청 종료 시간", example = "2024-05-20T10:00:00") java.time.LocalDateTime endedAt,

                @Schema(description = "플랫폼 타입 (OTT, YOUTUBE)", example = "OTT") PlatformType platform) {
}
