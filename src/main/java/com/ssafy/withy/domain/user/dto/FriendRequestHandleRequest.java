package com.ssafy.withy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 신청 수락/거절 요청")
public record FriendRequestHandleRequest(
        @Schema(description = "수락 여부 (true: 수락, false: 거절)", example = "true") Boolean isAccepted) {
}
