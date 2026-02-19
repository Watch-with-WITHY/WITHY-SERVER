package com.ssafy.withy.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSpoilerResult {
    private Integer chatLogId;
    private Boolean isSpoiler;
    private Double probability; // 신뢰도 등
}
