package com.ssafy.withy.domain.party.controller;

import com.ssafy.withy.domain.party.dto.extension.HandshakeResponse;
import com.ssafy.withy.domain.party.dto.extension.StateSyncDto;
import com.ssafy.withy.domain.party.entity.CommandType;
import com.ssafy.withy.domain.party.entity.Participant;
import com.ssafy.withy.domain.party.entity.ParticipantRole;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.entity.PartyState;
import com.ssafy.withy.domain.party.repository.ParticipantRepository;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import com.ssafy.withy.domain.party.repository.PartyStateRepository;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExtensionWebSocketControllerTest {

    @InjectMocks
    private ExtensionWebSocketController controller;

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private PartyStateRepository partyStateRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private com.ssafy.withy.domain.party.service.PartySessionManager partySessionManager;

    @Captor
    private ArgumentCaptor<HandshakeResponse> responseCaptor;

    private Principal principal;
    private User user;
    private Party party;
    private Participant participant;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        given(principal.getName()).willReturn("test@example.com");

        user = User.builder().id(1).email("test@example.com").build();
        party = Party.builder().id(100).build();
        participant = Participant.builder().user(user).party(party).role(ParticipantRole.HOST).build();
    }

    @Test
    @DisplayName("Host가 핸드셰이크 시 PartyState가 없으면 새로 생성하고 ACTIVE 반환")
    void handleHandshake_Host_CreatesState() {
        // given
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(participantRepository.findByPartyIdAndUserId(100, 1)).willReturn(Optional.of(participant));
        given(partyStateRepository.findById(100)).willReturn(Optional.empty());
        given(partyRepository.findById(100)).willReturn(Optional.of(party));

        org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor = mock(
                org.springframework.messaging.simp.SimpMessageHeaderAccessor.class);
        given(headerAccessor.getSessionId()).willReturn("session-123");

        // when
        controller.handleHandshake(100, principal, headerAccessor);

        // then
        verify(partyStateRepository).save(any(PartyState.class));

        verify(messagingTemplate).convertAndSendToUser(
                eq("test@example.com"),
                eq("/queue/party/100/extension/handshake"),
                responseCaptor.capture());

        HandshakeResponse response = responseCaptor.getValue();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getRole()).isEqualTo("HOST");
        assertThat(response.getInitialState().getCurrentPosition()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Guest가 핸드셰이크 시 PartyState가 없으면 WAITING 반환")
    void handleHandshake_Guest_Waiting() {
        // given
        participant = Participant.builder().user(user).party(party).role(ParticipantRole.GUEST).build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(participantRepository.findByPartyIdAndUserId(100, 1)).willReturn(Optional.of(participant));
        given(partyStateRepository.findById(100)).willReturn(Optional.empty());

        org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor = mock(
                org.springframework.messaging.simp.SimpMessageHeaderAccessor.class);
        given(headerAccessor.getSessionId()).willReturn("session-123");

        // when
        controller.handleHandshake(100, principal, headerAccessor);

        // then
        verify(partyStateRepository, never()).save(any());

        verify(messagingTemplate).convertAndSendToUser(
                eq("test@example.com"),
                eq("/queue/party/100/extension/handshake"),
                responseCaptor.capture());

        HandshakeResponse response = responseCaptor.getValue();
        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getInitialState()).isNull();
    }

    @Test
    @DisplayName("핸드셰이크 시 영상이 재생 중이면 시간 보정된 위치 반환")
    void handleHandshake_TimeCorrection() {
        // given
        LocalDateTime tenSecondsAgo = LocalDateTime.now().minusSeconds(10);
        PartyState existingState = PartyState.builder()
                .party(party)
                .isPlaying(true)
                .currentPosition(100)
                .updatedAt(tenSecondsAgo)
                .build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(participantRepository.findByPartyIdAndUserId(100, 1)).willReturn(Optional.of(participant));
        given(partyStateRepository.findById(100)).willReturn(Optional.of(existingState));

        org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor = mock(
                org.springframework.messaging.simp.SimpMessageHeaderAccessor.class);
        given(headerAccessor.getSessionId()).willReturn("session-123");

        // when
        controller.handleHandshake(100, principal, headerAccessor);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("test@example.com"),
                eq("/queue/party/100/extension/handshake"),
                responseCaptor.capture());

        HandshakeResponse response = responseCaptor.getValue();
        // 100초 + 10초 경과 = 110초 (오차 허용범위 내)
        assertThat(response.getInitialState().getCurrentPosition()).isBetween(109.0, 111.0);
    }

    @Test
    @DisplayName("Host가 상태 변경 시 DB 업데이트 및 브로드캐스트")
    void handleStateSync_Host_Updates() {
        // given
        StateSyncDto syncDto = StateSyncDto.builder()
                .commandType(CommandType.PLAY)
                .currentPosition(200.0)
                .isPlaying(true)
                .partyUrl("url")
                .timestamp(12345L)
                .build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(participantRepository.findByPartyIdAndUserId(100, 1)).willReturn(Optional.of(participant));
        given(partyRepository.findById(100)).willReturn(Optional.of(party));
        given(partyStateRepository.findById(100)).willReturn(Optional.empty());

        // when
        controller.handleStateSync(100, syncDto, principal);

        // then
        verify(partyStateRepository).save(any(PartyState.class));
        verify(messagingTemplate).convertAndSend(eq("/sub/party/100/extension"), eq(syncDto));
    }

    @Test
    @DisplayName("Guest가 상태 변경 시도 시 무시됨")
    void handleStateSync_Guest_Ignored() {
        // given
        participant = Participant.builder().user(user).party(party).role(ParticipantRole.GUEST).build();
        StateSyncDto syncDto = StateSyncDto.builder().commandType(CommandType.PLAY).build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(participantRepository.findByPartyIdAndUserId(100, 1)).willReturn(Optional.of(participant));

        // when
        controller.handleStateSync(100, syncDto, principal);

        // then
        verify(partyStateRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
