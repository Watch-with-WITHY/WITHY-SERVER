package com.ssafy.withy.domain.dm.dto;

import com.ssafy.withy.domain.dm.entity.DmRoom;
import com.ssafy.withy.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DmRoomResponse {
    
    private Integer roomId;
    private TargetUserInfo targetUser;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private Boolean isLeft;
    
    @Getter
    @Builder
    public static class TargetUserInfo {
        private Integer userId;
        private String nickname;
        private String profileImage;
    }
    
    /**
     * DmRoom 엔티티를 DmRoomResponse로 변환
     * @param room DM 방 엔티티
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return DmRoomResponse (상대방 정보만 포함)
     */
    public static DmRoomResponse from(DmRoom room, Integer currentUserId) {
        // currentUserId가 아닌 상대방을 targetUser로 설정
        User targetUser = room.getUserA().getId().equals(currentUserId) 
                          ? room.getUserB() : room.getUserA();
        
        return DmRoomResponse.builder()
                .roomId(room.getId())
                .targetUser(TargetUserInfo.builder()
                        .userId(targetUser.getId())
                        .nickname(targetUser.getNickname())
                        .profileImage(targetUser.getProfileImageUrl())
                        .build())
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .createdAt(room.getCreatedAt())
                .isLeft(room.isLeft(currentUserId))
                .build();
    }
}
