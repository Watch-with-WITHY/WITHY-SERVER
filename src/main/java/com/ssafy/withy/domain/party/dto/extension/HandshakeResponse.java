package com.ssafy.withy.domain.party.dto.extension;

import com.ssafy.withy.domain.party.entity.CommandType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HandshakeResponse {
    private String role; // "HOST" | "GUEST" | "MANAGER"
    private String status; // "ACTIVE" | "WAITING"
    private Long serverTime;
    private InitialState initialState;

    @Getter
    @Builder
    public static class InitialState {
        private Boolean isPlaying;
        private Double currentPosition;
        private CommandType lastCommand;
        private String partyUrl;
    }
}
