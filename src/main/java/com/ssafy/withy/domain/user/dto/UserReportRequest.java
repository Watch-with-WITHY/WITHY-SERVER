package com.ssafy.withy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 신고 요청")
public record UserReportRequest(
        @Schema(description = "신고할 채팅 메시지의 ID", example = "1") Integer chatId,

        @Schema(description = "신고 사유", example = "부적절한 언어를 사용했습니다.") String reason) {
}
