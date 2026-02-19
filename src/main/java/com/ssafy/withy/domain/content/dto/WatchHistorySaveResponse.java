package com.ssafy.withy.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시청 기록 저장 응답")
public record WatchHistorySaveResponse(
        @Schema(description = "저장된 시청 기록 ID", example = "456") Integer id) {
}
