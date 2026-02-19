package com.ssafy.withy.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PartyInvitationRequest(
        @Schema(description = "초대할 대상 유저 ID", example = "150")
        Integer targetUserId,
        
        @Schema(description = "함께 보낼 메시지 (선택 사항)", example = "야, 여기 들어와서 같이 보자!")
        String message
) {
}
