package com.ssafy.withy.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "채팅 번역 응답 DTO")
public class TranslationResponse {

    @Schema(description = "원본 메시지", example = "Hello, nice to meet you!")
    private String originalContent;

    @Schema(description = "번역된 메시지", example = "안녕하세요, 만나서 반가워요!")
    private String translatedContent;

    @Schema(description = "번역된 언어 코드", example = "ko")
    private String targetLang;

    public static TranslationResponse of(String originalContent, String translatedContent, String targetLang) {
        return TranslationResponse.builder()
                .originalContent(originalContent)
                .translatedContent(translatedContent)
                .targetLang(targetLang)
                .build();
    }
}
