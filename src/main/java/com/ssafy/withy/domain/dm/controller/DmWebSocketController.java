package com.ssafy.withy.domain.dm.controller;

import com.ssafy.withy.domain.dm.dto.DmMessageRequest;
import com.ssafy.withy.domain.dm.service.DmService;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DmWebSocketController {

    private final DmService dmService;
    private final UserRepository userRepository;

    /**
     * DM 메시지 전송 핸들러
     * 클라이언트 발행 경로: /pub/dm/message
     */
    @MessageMapping("/dm/message")
    @Transactional
    public void sendMessage(@Payload DmMessageRequest request, Principal principal) {
        if (principal == null) {
            log.warn("Unauthorized DM attempt");
            return;
        }

        String email = principal.getName();
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        log.info("DM Message ID: {}, Sender: {}, Room: {}", request.getMessage(), sender.getId(), request.getRoomId());

        dmService.sendMessage(sender.getId(), request);
    }
}
