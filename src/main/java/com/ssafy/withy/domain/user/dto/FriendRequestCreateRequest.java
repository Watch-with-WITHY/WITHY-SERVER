package com.ssafy.withy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 신청 발송 요청")
public record FriendRequestCreateRequest(
        @Schema(description = "친구 신청을 받을 사용자의 ID", example = "3") Integer receiverId) {
}
