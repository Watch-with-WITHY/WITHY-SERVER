package com.ssafy.withy.domain.chat.service;

import com.ssafy.withy.domain.chat.dto.ChatRequest;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.domain.chat.dto.ChatResponse;
import com.ssafy.withy.domain.chat.entity.ChatLog;
import com.ssafy.withy.domain.chat.entity.MessageType;
import com.ssafy.withy.domain.chat.repository.ChatRepository;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.domain.party.entity.Participant;
import com.ssafy.withy.domain.party.entity.ParticipantRole;
import com.ssafy.withy.domain.party.repository.ParticipantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private AggressionClient aggressionClient;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParticipantRepository participantRepository;

    @Test
    @DisplayName("정상 메시지 전송: 검사 통과 -> 저장 -> 전송 -> 버퍼링")
    void processMessage_Success() {
        // given
        Integer partyId = 1;
        Integer userId = 101;
        String content = "안녕하세요";
        ChatRequest request = new ChatRequest(partyId, userId, content, MessageType.TEXT);

        // Mocking
        given(aggressionClient.checkAggression(content, userId, partyId)).willReturn(false); // 공격성 없음
        given(partyRepository.findById(1)).willReturn(Optional.of(Party.builder().id(1).build()));
        given(userRepository.findById(101)).willReturn(Optional.of(User.builder().id(101).build()));

        ChatLog savedLog = ChatLog.builder()
                .id(1)
                .party(Party.builder().id(1).build())
                .user(User.builder().id(101).build())
                .message(content)
                .messageType(MessageType.TEXT)
                .isSpoiler(false)
                .build();
        given(chatRepository.save(any(ChatLog.class))).willReturn(savedLog);

        // when
        chatService.processMessage(request);

        // then
        // 1. 공격성 통과 확인
        verify(aggressionClient, times(1)).checkAggression(content, userId, partyId);
        // 2. 저장 확인
        verify(chatRepository, times(1)).save(any(ChatLog.class));
        // 3. WebSocket 전송 확인
        verify(messagingTemplate, times(1)).convertAndSend(eq("/sub/chat/room/1"), any(ChatResponse.class));
        // 4. Redis 버퍼링 확인
        verify(redisService, times(1)).bufferAndPublish(any(ChatResponse.class));
    }

//    @Test
//    @DisplayName("공격성 메시지 감지 시 예외 발생 및 중단")
//    void processMessage_AggressionDetected() {
//        // given
//        ChatRequest request = new ChatRequest(1, 101, "나쁜말", MessageType.TEXT);
//        given(aggressionClient.checkAggression("나쁜말", 101, 1)).willReturn(true); // 공격성 있음
//
//        // when & then
//        assertThatThrownBy(() -> chatService.processMessage(request))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("공격성 언어");
//
//        // 뒷단 로직 실행 안 됨을 검증
//        verify(chatRepository, never()).save(any());
//        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
//    }

    @Test
    @DisplayName("이모지 타입 메시지도 정상적으로 처리되어야 한다")
    void processMessage_EmojiSuccess() {
        // given
        Integer partyId = 1;
        Integer userId = 101;
        String content = "😊";
        ChatRequest request = new ChatRequest(partyId, userId, content, MessageType.EMOJI);

        given(aggressionClient.checkAggression(content, userId, partyId)).willReturn(false);
        given(partyRepository.findById(1)).willReturn(Optional.of(Party.builder().id(1).build()));
        given(userRepository.findById(101)).willReturn(Optional.of(User.builder().id(101).build()));

        ChatLog savedLog = ChatLog.builder()
                .id(2)
                .party(Party.builder().id(1).build())
                .user(User.builder().id(101).build())
                .message(content)
                .messageType(MessageType.EMOJI)
                .isSpoiler(false)
                .build();
        given(chatRepository.save(any(ChatLog.class))).willReturn(savedLog);

        // when
        chatService.processMessage(request);

        // then
        // EMOJI 타입 검증
        verify(chatRepository).save(argThat(log -> log.getMessageType() == MessageType.EMOJI));
    }

    @Test
    @DisplayName("존재하지 않는 유저나 파티 ID로 요청 시 예외가 발생해야 한다")
    void processMessage_NotFoundEntity() {
        // given
        Integer invalidPartyId = 999;
        ChatRequest request = new ChatRequest(invalidPartyId, 101, "test", MessageType.TEXT);

        given(aggressionClient.checkAggression("test", 101, invalidPartyId)).willReturn(false);
        given(partyRepository.findById(999)).willReturn(Optional.empty()); // Party 없음

        // when & then
        assertThatThrownBy(() -> chatService.processMessage(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.ENTITY_NOT_FOUND);

        verify(chatRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅 삭제 성공 - 본인 작성")
    void deleteChat_Success_Owner() {
        // given
        Integer chatId = 100;
        Integer userId = 101;

        ChatLog chatLog = ChatLog.builder()
                .id(chatId)
                .user(User.builder().id(userId).build())
                .party(Party.builder().id(1).build())
                .message("삭제할 메시지")
                .isDeleted(false)
                .build();

        given(chatRepository.findById(chatId)).willReturn(Optional.of(chatLog));

        // when
        chatService.deleteChat(chatId, userId);

        // then
        verify(chatRepository).save(argThat(log -> log.getIsDeleted() == true));
        verify(messagingTemplate).convertAndSend(eq("/sub/chat/room/1"), any(ChatResponse.class));
    }

    @Test
    @DisplayName("채팅 삭제 성공 - 방장 권한")
    void deleteChat_Success_Host() {
        // given
        Integer chatId = 100;
        Integer userId = 202; // Host
        Integer writerId = 101; // Guest

        ChatLog chatLog = ChatLog.builder()
                .id(chatId)
                .user(User.builder().id(writerId).build())
                .party(Party.builder().id(1).build())
                .isDeleted(false)
                .build();

        Participant hostParticipant = Participant.builder()
                .user(User.builder().id(userId).build())
                .party(Party.builder().id(1).build())
                .role(ParticipantRole.HOST)
                .build();

        given(chatRepository.findById(chatId)).willReturn(Optional.of(chatLog));
        given(participantRepository.findByParty_IdAndUser_Id(1, userId))
                .willReturn(Optional.of(hostParticipant));

        // when
        chatService.deleteChat(chatId, userId);

        // then
        verify(chatRepository).save(argThat(log -> log.getIsDeleted() == true));
    }

    @Test
    @DisplayName("채팅 삭제 실패 - 권한 없음 (타인 메시지 삭제 시도)")
    void deleteChat_Fail_NoPermission() {
        // given
        Integer chatId = 100;
        Integer userId = 303; // Guest (Not Owner)
        Integer writerId = 101;

        ChatLog chatLog = ChatLog.builder()
                .id(chatId)
                .user(User.builder().id(writerId).build())
                .party(Party.builder().id(1).build())
                .isDeleted(false)
                .build();

        Participant guestParticipant = Participant.builder()
                .user(User.builder().id(userId).build())
                .role(ParticipantRole.GUEST)
                .build();

        given(chatRepository.findById(chatId)).willReturn(Optional.of(chatLog));
        given(participantRepository.findByParty_IdAndUser_Id(1, userId))
                .willReturn(Optional.of(guestParticipant));

        // when & then
        assertThatThrownBy(() -> chatService.deleteChat(chatId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.CHAT_DELETE_FORBIDDEN);
        
        verify(chatRepository, never()).save(argThat(log -> log.getIsDeleted() == true));
    }
}
