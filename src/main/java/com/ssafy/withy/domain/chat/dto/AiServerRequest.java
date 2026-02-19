package com.ssafy.withy.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiServerRequest {

    @JsonProperty("chat_id")
    private String chatId;

    @JsonProperty("party_id")
    private String partyId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("content")
    private String content;

    @JsonProperty("target_lang")
    private String targetLang;

    public static AiServerRequest of(String chatId, String partyId, String userId, String content, String targetLang) {
        return AiServerRequest.builder()
                .chatId(chatId)
                .partyId(partyId)
                .userId(userId)
                .content(content)
                .targetLang(targetLang)
                .build();
    }
}
