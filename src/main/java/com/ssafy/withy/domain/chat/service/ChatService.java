package com.ssafy.withy.domain.chat.service;

import com.ssafy.withy.domain.chat.dto.ChatRequest;
import com.ssafy.withy.domain.chat.dto.ChatResponse;
import com.ssafy.withy.domain.chat.entity.ChatLog;
import com.ssafy.withy.domain.chat.repository.ChatRepository;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.entity.Participant;
import com.ssafy.withy.domain.party.repository.ParticipantRepository;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final AggressionClient aggressionClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRepository chatRepository;
    private final RedisService redisService;
    private final PartyRepository partyRepository; // 필요 시 구현
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository; // 필요 시 구현

    /**
     * 채팅 메시지 처리 메인 로직
     * 1. gRPC 공격성 검사 (Pre-send)
     * 2. (Async) DB 저장
     * 3. WebSocket 전송
     * 4. (Async) Redis 버퍼링 -> (5개 묶음) -> Kafka 전송
     */
    @Transactional
    public void processMessage(ChatRequest dto) {
        // 1. gRPC 공격성 검사 (Blocking, 100ms limit)
        boolean isBad = aggressionClient.checkAggression(dto.getContent(), dto.getUserId(), Math.toIntExact(dto.getPartyId()));
        String finalContent = dto.getContent();
        boolean isProfanity = false;

        if (isBad) {
            finalContent = "🚫 공격성 언어가 감지되어 숨겨진 메시지입니다.";
            isProfanity = true;
        }

        // 엔티티 조회 (캐싱되어 있다고 가정하거나, 단순 참조만 필요하면 getReference)
        Party party = partyRepository.findById(Math.toIntExact(dto.getPartyId()))
                .orElseThrow(() -> new CustomException(GlobalErrorCode.ENTITY_NOT_FOUND));
        User user = userRepository.findById(Math.toIntExact(dto.getUserId()))
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // ChatLog 엔티티 생성
        ChatLog chatLog = ChatLog.builder()
                .party(party)
                .user(user)
                .message(finalContent)
                .messageType(dto.getType())
                .isSpoiler(false) // 아직 모름 (Kafka->AI 후 판별)
                .isProfanity(isProfanity) // gRPC 결과 반영
                .createdAt(LocalDateTime.now())
                .build();

        // 2. DB 저장 (비동기로 처리하여 Latency 감소 추천, 여기서는 @Async 메소드로 분리)
        ChatLog saved = saveChatLog(chatLog);

        // Response DTO 변환
        ChatResponse response = ChatResponse.from(saved);

        // 3. WebSocket 전송 (Broadcasting)
        // 구독 경로: /sub/chat/room/{partyId}
        try {
            messagingTemplate.convertAndSend("/sub/chat/room/" + dto.getPartyId(), response);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message", e);
        }

        // 4. Redis 버퍼링 및 Kafka 파이프라인 (비동기 권장)
        // 이 메소드 자체가 @Async가 아니라면, 내부에서 비동기 호출하거나, RedisService를 비동기로 부름
        // Kafka 연결 실패 시에도 채팅 자체는 성립되도록 try-catch 처리
        try {
            if (!isProfanity) {
                redisService.bufferAndPublish(response);
            }
        } catch (Exception e) {
            log.error("Redis/Kafka pipeline failed (Non-blocking)", e);
        }
    }

    // 별도 트랜잭션으로 분리하거나 비동기 처리 가능
    public ChatLog saveChatLog(ChatLog chatLog) {
        return chatRepository.save(chatLog);
    }

    @Transactional
    public void deleteChat(Integer chatId, Integer userId) {
        ChatLog chatLog = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        boolean isWriter = chatLog.getUser().getId().equals(userId);
        
        if (!isWriter) {
            // 작성자가 아니면 방장/매니저 권한 확인
            Participant participant = participantRepository.findByParty_IdAndUser_Id(
                    chatLog.getParty().getId(), userId)
                    .orElseThrow(() -> new CustomException(GlobalErrorCode.ACCESS_DENIED));
            
            boolean isAdmin = participant.getRole() == com.ssafy.withy.domain.party.entity.ParticipantRole.HOST 
                           || participant.getRole() == com.ssafy.withy.domain.party.entity.ParticipantRole.MANAGER;

            if (!isAdmin) {
                throw new CustomException(GlobalErrorCode.CHAT_DELETE_FORBIDDEN);
            }
        }

        // Soft Delete
        ChatLog deletedLog = chatLog.toBuilder().isDeleted(true).build();
        chatRepository.save(deletedLog);

        // Broadcast Deletion
        ChatResponse response = ChatResponse.from(deletedLog);
        messagingTemplate.convertAndSend("/sub/chat/room/" + chatLog.getParty().getId(), response);
    }
}
