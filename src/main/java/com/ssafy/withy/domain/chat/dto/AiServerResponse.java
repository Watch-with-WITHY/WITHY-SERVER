package com.ssafy.withy.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServerResponse {

    @JsonProperty("chat_id")
    private String chatId;

    @JsonProperty("party_id")
    private String partyId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("translated_content")
    private String translatedContent;
}
