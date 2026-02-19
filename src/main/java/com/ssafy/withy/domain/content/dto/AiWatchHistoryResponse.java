package com.ssafy.withy.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "AI 전용 시청 기록 응답 (간소화)")
public record AiWatchHistoryResponse(
        @Schema(description = "컨텐츠 ID", example = "55") Integer contentId,
        @Schema(description = "시청 종료 시간", example = "2026-02-05T09:00:00") LocalDateTime endedAt
) {
}
