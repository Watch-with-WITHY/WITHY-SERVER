package com.ssafy.withy.domain.party.event;

import com.ssafy.withy.domain.party.service.PartyService;
import com.ssafy.withy.domain.party.service.PartySessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PartySessionManager partySessionManager;
    private final PartyService partyService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        Integer partyId = partySessionManager.getPartyId(sessionId);
        Integer userId = partySessionManager.getUserId(sessionId);

        if (partyId != null && userId != null) {
            log.info("User Disconnected: PartyId={}, UserId={}", partyId, userId);

            // 세션 정보 삭제
            partySessionManager.removeSession(sessionId);

            // 아직 해당 유저의 다른 세션이 남아있는지 확인 (멀티 탭 지원)
            boolean isStillOnline = partySessionManager.isUserOnline(partyId, userId);

            if (!isStillOnline) {
                // 연결 해제 처리 위임 (Host: Grace Period, Guest: Leave)
                partyService.handleDisconnect(partyId, userId);
            }
        }
    }
}
