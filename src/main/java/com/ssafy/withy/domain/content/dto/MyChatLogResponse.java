package com.ssafy.withy.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 채팅 로그 응답")
public record MyChatLogResponse(
        @Schema(description = "채팅 로그 ID", example = "789") Integer id,

        @Schema(description = "채팅 내용", example = "안녕하세요! 반갑습니다.") String message,

        @Schema(description = "파티 제목", example = "오징어 게임 같이 봐요") String partyTitle,

        @Schema(description = "파티 ID", example = "12") Integer partyId,

        @Schema(description = "작성 시간", example = "2024-05-20T10:00:00") String createdAt) {
}
