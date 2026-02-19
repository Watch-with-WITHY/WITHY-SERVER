package com.ssafy.withy.domain.user.dto;

import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 프로필 응답")
public record UserProfileResponse(
        @Schema(description = "유저 ID", example = "5") Integer id,
        @Schema(description = "닉네임", example = "HappyUser") String nickname,
        @Schema(description = "프로필 이미지 URL", example = "http://example.com/profile.jpg") String profileImageUrl,
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "언어", example = "ko") String preferredLanguage,
        @Schema(description = "접속 상태", example = "ONLINE") UserStatus status) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getEmail(),
                user.getPreferredLanguage(),
                user.getStatus());
    }
}
