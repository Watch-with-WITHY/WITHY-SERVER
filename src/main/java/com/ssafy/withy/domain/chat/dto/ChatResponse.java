package com.ssafy.withy.domain.chat.dto;

import com.ssafy.withy.domain.chat.entity.ChatLog;
import com.ssafy.withy.domain.chat.entity.MessageType;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Integer id;
    private Integer partyId;
    private Integer userId;
    private String nickname;
    private String content;
    private MessageType type;
    private Boolean isSpoiler;
    private Boolean isProfanity;
    private Boolean isDeleted;
    private LocalDateTime createdAt;

    public static ChatResponse from(ChatLog chatLog) {
        return ChatResponse.builder()
                .id(chatLog.getId())
                // Party나 User 참조는 LAZY 로딩 이슈가 있을 수 있으므로 ID만 사용하거나 별도 매핑 필요
                // 여기서는 엔티티가 이미 로딩되었다고 가정하거나, 엔티티 ID 접근이 가능하다고( getId()) 가정
                .partyId(chatLog.getParty().getId())
                .userId(chatLog.getUser().getId())
                .nickname(chatLog.getUser().getNickname())
                .content(chatLog.getMessage())
                .type(chatLog.getMessageType())
                .isSpoiler(chatLog.getIsSpoiler())
                .isProfanity(chatLog.getIsProfanity())
                .isDeleted(chatLog.getIsDeleted())
                .createdAt(chatLog.getCreatedAt())
                .build();
    }
}
