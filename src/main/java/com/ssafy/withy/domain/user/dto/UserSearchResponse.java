package com.ssafy.withy.domain.user.dto;

import com.ssafy.withy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserSearchResponse(
        @Schema(description = "유저 ID", example = "1")
        Integer userId,

        @Schema(description = "닉네임", example = "승호짱")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://s3...")
        String profileImage,

        @Schema(description = "이미 친구인지 여부 (true: 친구임, false: 남남)", example = "false")
        boolean isFriend,

        @Schema(description = "이미 친구 신청을 보냈는지 여부", example = "false")
        boolean isFriendRequestSent
) {
    public static UserSearchResponse from(User user, boolean isFriend, boolean isFriendRequestSent) {
        return new UserSearchResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                isFriend,
                isFriendRequestSent
        );
    }
}
