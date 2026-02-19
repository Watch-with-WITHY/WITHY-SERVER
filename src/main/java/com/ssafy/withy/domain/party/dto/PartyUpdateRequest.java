package com.ssafy.withy.domain.party.dto;

import com.ssafy.withy.domain.party.entity.PlatformType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PartyUpdateRequest(
        @NotBlank String title,
        @Min(2) @Max(100) int maxParticipants,
        boolean isPrivate,
        String password, // 비공개일 때만 필수

        String contentId,
        String contentTitle,
        PlatformType platform
) {}