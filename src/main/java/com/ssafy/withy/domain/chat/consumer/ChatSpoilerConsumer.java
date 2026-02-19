package com.ssafy.withy.domain.chat.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.withy.domain.chat.dto.ChatLogDto;
import com.ssafy.withy.domain.chat.dto.ChatResponse;
import com.ssafy.withy.domain.chat.entity.ChatLog; // 추가
import com.ssafy.withy.domain.chat.entity.MessageType;
import com.ssafy.withy.domain.chat.repository.ChatRepository; // 추가
import com.ssafy.withy.domain.chat.service.RedisService; // 추가
import com.ssafy.withy.domain.chat.dto.SpoilerCheckResponse;
import com.ssafy.withy.domain.chat.service.SpoilerClient;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSpoilerConsumer {

    private final ObjectMapper objectMapper;
    private final PartyRepository partyRepository;
    private final ChatRepository chatRepository;
    private final SpoilerClient spoilerClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisService redisService; // 추가

    @KafkaListener(topics = "spoiler-check", groupId = "withy-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(List<String> messages) {
        log.info("Consumed {} messages for spoiler check", messages.size());

        List<ChatResponse> chatList = new ArrayList<>();
        for (String msg : messages) {
            if (msg == null) {
                continue;
            }
            try {
                // 로그 추가: 수신된 원본 메시지 확인
                log.debug("Received raw Kafka message: {}", msg);
                ChatResponse chat = objectMapper.readValue(msg, ChatResponse.class);
                chatList.add(chat);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse chat message: {}", msg, e);
            }
        }

        if (chatList.isEmpty()) return;

        // PartyId 별로 그룹화 (배치 처리를 위해)
        Map<Integer, List<ChatResponse>> groupedByParty = chatList.stream()
                .collect(Collectors.groupingBy(ChatResponse::getPartyId));

        groupedByParty.forEach((partyId, chats) -> {
            try {
                processPartyMessages(partyId, chats);
            } catch (Exception e) {
                log.error("Error processing messages for party {}", partyId, e);
            }
        });
    }

    private void processPartyMessages(Integer partyId, List<ChatResponse> chats) {
        log.info("Processing spoiler check for Party ID: {}, Count: {}", partyId, chats.size());

        // 1. 영화 줄거리 조회 (Redis Caching 적용)
        String moviePlot = redisService.getCachedPlot(partyId, () -> {
            Party party = partyRepository.findWithContentById(partyId);
            if (party == null || party.getContent() == null || party.getContent().getOverview() == null) {
                log.warn("Overview not found for party {}", partyId);
                return null;
            }
            return party.getContent().getOverview();
        });

        if (moviePlot == null) {
            return;
        }

        // 2. Request DTO 변환
        List<ChatLogDto> dtos = chats.stream()
                .map(c -> ChatLogDto.of(
                        String.valueOf(c.getId()),
                        String.valueOf(c.getUserId()),
                        c.getContent()
                ))
                .collect(Collectors.toList());

        // 3. AI 서버 요청 (REST)
        SpoilerCheckResponse response = spoilerClient.checkSpoiler(moviePlot, String.valueOf(partyId), dtos);

        if (response == null) {
            log.warn("Spoiler check response is NULL for party {}", partyId);
            return;
        }

        if (response.getResults() == null) {
            log.warn("Spoiler check response results list is NULL for party {}", partyId);
            return;
        }

        log.info("Spoiler check result received. Count: {}", response.getResults().size());

        // 4. 결과 처리
        for (SpoilerCheckResponse.SpoilerResult result : response.getResults()) {
            if (Boolean.TRUE.equals(result.getIsSpoiler())) {
                handleSpoiler(partyId, result);
            }
        }
    }

    private void handleSpoiler(Integer partyId, SpoilerCheckResponse.SpoilerResult result) {
        log.info("Spoiler detected! Party: {}, MsgID(raw): {}, UserID(raw): {}, Reason: {}",
                partyId, result.getId(), result.getUserId(), result.getReason());

        Integer chatId = Integer.parseInt(result.getId());
        ChatLog chatLog = chatRepository.findByIdWithUser(chatId).orElse(null);

        String nickname = "Unknown";
        if (chatLog != null) {
            ChatLog updatedLog = chatLog.toBuilder()
                    .isSpoiler(true)
                    .build();
            chatRepository.save(updatedLog);

            nickname = chatLog.getUser().getNickname();
        }

        ChatResponse updateMessage = ChatResponse.builder()
                .id(chatId)
                .partyId(partyId)
                .userId(Integer.parseInt(result.getUserId()))
                .nickname(nickname)
                .content(chatLog != null ? chatLog.getMessage() : "내용 없음") // 기본 표시는 마스킹 메시지
                .isSpoiler(true)
                .type(MessageType.TEXT)
                .build();

        try {
            messagingTemplate.convertAndSend("/sub/chat/room/" + partyId, updateMessage);
            log.info("Broadcasted spoiler alert to /sub/chat/room/{}", partyId);
        } catch (Exception e) {
            log.error("Failed to send spoiler update to websocket", e);
        }
    }
}
