package com.ssafy.withy.domain.user.dto;

import com.ssafy.withy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "차단 유저 정보 응답")
public record BlockListResponse(
        @Schema(description = "차단된 유저 ID", example = "10") Integer id,
        @Schema(description = "닉네임", example = "BadUser") String nickname,
        @Schema(description = "프로필 이미지 URL", example = "http://example.com/bad.jpg") String profileImageUrl) {
    public static BlockListResponse from(User user) {
        return new BlockListResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl());
    }
}
