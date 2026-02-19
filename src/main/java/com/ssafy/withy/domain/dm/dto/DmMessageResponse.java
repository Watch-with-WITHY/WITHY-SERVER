package com.ssafy.withy.domain.dm.dto;

import com.ssafy.withy.domain.dm.entity.DmMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DmMessageResponse {
    
    private Integer messageId;
    private Integer senderId;
    private String senderNickname;
    private String message;
    private LocalDateTime createdAt;
    
    /**
     * DmMessage 엔티티를 DmMessageResponse로 변환
     * @param message DM 메시지 엔티티
     * @return DmMessageResponse
     */
    public static DmMessageResponse from(DmMessage message) {
        return DmMessageResponse.builder()
                .messageId(message.getId())
                .senderId(message.getSender().getId())
                .senderNickname(message.getSender().getNickname())
                .message(message.getMessage())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
