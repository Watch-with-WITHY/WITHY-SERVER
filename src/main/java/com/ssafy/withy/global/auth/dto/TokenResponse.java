package com.ssafy.withy.global.auth.dto;

import com.ssafy.withy.domain.user.entity.LoginType;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        Integer userId,
        String nickname,
        boolean isOnboardingComplete,
        LoginType loginType
) {}