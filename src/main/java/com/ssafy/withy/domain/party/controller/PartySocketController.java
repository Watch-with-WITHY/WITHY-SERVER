package com.ssafy.withy.domain.party.controller;

import com.ssafy.withy.domain.party.dto.PartySyncRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PartySocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 주기적 재생 시점 동기화 (Heartbeat) - 5초마다 호스트가 전송
     * 경로: /pub/party/sync
     * 수신: /sub/party/{partyId}/player
     */
    @MessageMapping("/party/sync")
    public void syncPlaytime(@Payload PartySyncRequest request) {
        // 호스트 검증 로직을 추가할 수도 있으나, 빈번항 요청의 성능을 위해 생략하고
        // 프론트에서 호스트일 때만 보내도록 구현하는 것을 권장.
        // 필요 시 DB 조회 없이 Security Context나 세션으로 검증 가능.

        // 그대로 구독자들에게 브로드캐스팅
        messagingTemplate.convertAndSend("/sub/party/" + request.getPartyId() + "/player", request);
    }
}
