package com.ssafy.withy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 차단 요청")
public record BlockUserRequest(
        @Schema(description = "차단할 유저 ID", example = "123") Integer blockedId) {
}
