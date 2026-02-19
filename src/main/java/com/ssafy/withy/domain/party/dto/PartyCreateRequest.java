package com.ssafy.withy.domain.party.dto;

import com.ssafy.withy.domain.party.entity.PlatformType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PartyCreateRequest(
        @NotBlank(message = "파티 제목은 필수입니다.")
        String title,

        @NotBlank(message = "컨텐츠 ID는 필수입니다.")
        String contentId, // "80057281" or "VideoID"

        @NotBlank(message = "컨텐츠 제목은 필수입니다.")
        String contentTitle, // "Stranger Things" or "Youtube Video Title"

        @NotNull(message = "플랫폼 타입은 필수입니다.")
        PlatformType platform, // OTT, YOUTUBE

        @NotNull(message = "시작 시간은 필수입니다.")
        LocalDateTime scheduledActiveTime, // 파티 시작 시간

        @Min(2) @Max(100) // 인원 제한 (최소 2명, 최대 100명)
        Integer maxParticipants,

        Boolean isPrivate, // 비공개 여부
        String password    // 비공개일 때만 값 있음
) {}