package com.ssafy.withy.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        String email
) {
}
