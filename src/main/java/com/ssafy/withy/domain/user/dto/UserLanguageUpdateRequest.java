package com.ssafy.withy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "유저 선호 언어 변경 요청")
public record UserLanguageUpdateRequest(
        @Schema(description = "변경할 언어 코드 (예: ko, en, ja)", example = "en")
        @NotBlank(message = "언어 코드는 필수입니다.")
        @Size(min = 2, max = 10, message = "유효하지 않은 언어 코드입니다.")
        String language
) {}