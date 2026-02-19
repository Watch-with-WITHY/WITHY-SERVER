package com.ssafy.withy.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatLogDto {
    @JsonProperty("id")
    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("content")
    private String content;

    public static ChatLogDto of(String id, String userId, String content) {
        return ChatLogDto.builder()
                .id(id)
                .userId(userId)
                .content(content)
                .build();
    }
}
