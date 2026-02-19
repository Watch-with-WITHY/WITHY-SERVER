package com.ssafy.withy.domain.auth.dto;

public record EmailVerifyRequest(
        String email,
        String code
) {
}
