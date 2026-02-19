package com.ssafy.withy.domain.user.dto;

import com.ssafy.withy.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 정보 응답")
public record FriendDto(
        @Schema(description = "친구 사용자 ID", example = "5") Integer userId,
        @Schema(description = "친구 닉네임", example = "friendUser") String nickname,
        @Schema(description = "친구 프로필 이미지 URL", example = "http://example.com/friend.jpg") String profileImageUrl,
        @Schema(description = "친구 접속 상태", example = "ONLINE") UserStatus status) {
}
