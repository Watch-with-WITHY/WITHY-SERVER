package com.ssafy.withy.domain.party.dto;

import com.ssafy.withy.domain.party.entity.Participant;
import com.ssafy.withy.domain.party.entity.ParticipantRole;
import com.ssafy.withy.domain.party.entity.ParticipantStatus;
import lombok.Builder;

@Builder
public record ParticipantResponse(
        Integer userId,
        String nickname,
        String profileImage,
        ParticipantRole role, // HOST, MANAGER, GUEST
        ParticipantStatus status, // JOINED, BANNED, MUTED
        boolean isOnline) {
    public static ParticipantResponse from(Participant participant, boolean isOnline) {
        return ParticipantResponse.builder()
                .userId(participant.getUser().getId())
                .nickname(participant.getUser().getNickname())
                .profileImage(participant.getUser().getProfileImageUrl())
                .role(participant.getRole())
                .status(participant.getStatus())
                .isOnline(isOnline)
                .build();
    }
}