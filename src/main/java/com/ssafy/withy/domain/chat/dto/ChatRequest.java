package com.ssafy.withy.domain.chat.dto;

import com.ssafy.withy.domain.chat.entity.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private Integer partyId;
    private Integer userId;
    private String content;
    private MessageType type;
}
