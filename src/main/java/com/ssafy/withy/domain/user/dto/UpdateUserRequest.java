package com.ssafy.withy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 프로필 수정 요청")
public record UpdateUserRequest(
        @Schema(description = "변경할 닉네임", example = "newNickname") String nickname) {
}
