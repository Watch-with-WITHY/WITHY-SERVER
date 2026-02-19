package com.ssafy.withy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "받은 친구 신청 응답")
@Builder
public record FriendRequestResponse(
        @Schema(description = "친구 신청 ID", example = "15") Integer requestId,
        @Schema(description = "신청자 ID", example = "2") Integer requesterId,
        @Schema(description = "신청자 닉네임", example = "requestUser") String requesterNickname,
        @Schema(description = "신청자 프로필 이미지 URL", example = "http://example.com/profile.jpg") String requesterProfileImageUrl) {
}
