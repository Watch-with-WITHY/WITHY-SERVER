package com.ssafy.withy.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "채팅 번역 요청 DTO")
public class TranslationRequest {

    @Schema(description = "채팅 메시지 ID (ChatLog)", example = "101")
    @NotNull(message = "채팅 ID는 필수입니다.")
    private Integer chatId;

    @Schema(description = "파티 ID", example = "1")
    @NotNull(message = "파티 ID는 필수입니다.")
    private Integer partyId;

    @Schema(description = "번역할 원본 메시지", example = "Hello, nice to meet you!")
    @NotBlank(message = "번역할 내용은 필수입니다.")
    private String content;

    @Schema(description = "목표 언어 코드 (기본값: ko)", example = "ko")
    @Builder.Default
    private String targetLang = "ko";
}
