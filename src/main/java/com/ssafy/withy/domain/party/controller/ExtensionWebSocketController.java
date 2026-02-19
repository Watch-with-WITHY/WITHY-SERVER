package com.ssafy.withy.domain.party.controller;

import com.ssafy.withy.domain.party.dto.extension.HandshakeResponse;
import com.ssafy.withy.domain.party.dto.extension.StateSyncDto;
import com.ssafy.withy.domain.party.entity.*;
import com.ssafy.withy.domain.party.repository.ParticipantRepository;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import com.ssafy.withy.domain.party.repository.PartyStateRepository;
import com.ssafy.withy.domain.party.service.PartySessionManager;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ExtensionWebSocketController {

    private final PartyRepository partyRepository;
    private final PartyStateRepository partyStateRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PartySessionManager partySessionManager;

    /**
     * Handshake 시 sessionId 확보를 위해 HeaderAccessor 추가
     */
    @MessageMapping("/party/{partyId}/extension/handshake")
    @Transactional
    public void handleHandshake(@DestinationVariable Integer partyId, Principal principal,
                                SimpMessageHeaderAccessor headerAccessor) {
        if (principal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED_USER);
        }

        User user = getUserByPrincipal(principal);
        Participant participant = participantRepository.findByPartyIdAndUserId(partyId, user.getId())
                .orElseGet(() -> {
                    // 참여자가 아니면 GUEST로 자동 가입
                    Party party = partyRepository.findById(partyId)
                            .orElseThrow(() -> new CustomException(
                                    GlobalErrorCode.PARTY_NOT_FOUND));

                    Participant newGuest = Participant.builder()
                            .party(party)
                            .user(user)
                            .role(ParticipantRole.GUEST)
                            .status(ParticipantStatus.JOINED)
                            .build();
                    participantRepository.save(newGuest);
                    log.info("Auto-joined user {} to party {} as GUEST", user.getId(), partyId);

                    // DB Count 증가 필요 (Party 엔티티에 메서드 필요하거나 직접 수정)
                    party.increaseCurrentParticipants();

                    return newGuest;
                });

        // 세션 등록
        partySessionManager.registerSession(headerAccessor.getSessionId(), partyId, user.getId());

        // 입장 알림 브로드캐스팅 (나를 제외한 나머지에게? 아니면 전체에게?)
        // 전체에게 보냄. 프론트에서 중복 처리하거나 내꺼면 무시.
        var joinMessage = Map.of(
                "type", "USER_JOINED",
                "userId", user.getId(),
                "nickname", user.getNickname() != null ? user.getNickname() : "Unknown",
                "profileImage", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "");
        messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", joinMessage);

        // 파티 상태 조회
        PartyState partyState = partyStateRepository.findById(partyId).orElse(null);
        long serverTime = System.currentTimeMillis();

        // 1. HOST인 경우: PartyState가 없으면 생성 (시작 시점)
        if (participant.getRole() == ParticipantRole.HOST) {
            // 호스트가 재접속했으므로 삭제 예약 취소
            partySessionManager.cancelHostCleanup(partyId);

            if (partyState == null) {
                Party party = partyRepository.findById(partyId)
                        .orElseThrow(() -> new CustomException(
                                GlobalErrorCode.PARTY_NOT_FOUND));

                partyState = PartyState.builder()
                        .party(party)
                        .partyUrl("") // 초기 URL 없음
                        .lastCommand(CommandType.PAUSE) // 초기 정지 상태
                        .currentPosition(0)
                        .isPlaying(false)
                        .updatedAt(LocalDateTime.now())
                        .build();
                partyStateRepository.save(partyState);
                log.info("PartyState created for partyId: {}", partyId);
            }
        }

        HandshakeResponse response;

        // 2. PartyState가 없는 경우 (Guest 입장 시 아직 Host가 시작 안함)
        if (partyState == null) {
            response = HandshakeResponse.builder()
                    .role(participant.getRole().name())
                    .status("WAITING")
                    .serverTime(serverTime)
                    .initialState(null)
                    .build();
        } else {
            // 3. 시간 보정 계산
            double currentPosition = partyState.getCurrentPosition();
            if (partyState.getIsPlaying()) {
                long elapsedSeconds = Duration.between(partyState.getUpdatedAt(), LocalDateTime.now())
                        .getSeconds();
                currentPosition += elapsedSeconds;
            }

            response = HandshakeResponse.builder()
                    .role(participant.getRole().name())
                    .status("ACTIVE")
                    .serverTime(serverTime)
                    .initialState(HandshakeResponse.InitialState.builder()
                            .isPlaying(partyState.getIsPlaying())
                            .currentPosition(currentPosition)
                            .lastCommand(partyState.getLastCommand())
                            .partyUrl(partyState.getPartyUrl())
                            .build())
                    .build();
        }

        // 특정 유저에게 1:1 메시지 전송
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/party/" + partyId + "/extension/handshake",
                response);
        log.info("Sent Handshake response to user: {}", principal.getName());

        // [DEBUG] 브로드캐스팅으로도 보내서 확인 (나중에 제거)
        messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", response);
        log.info("[DEBUG] Broadcasted Handshake response for debugging");
    }

    /**
     * 상태 동기화 요청 처리
     * 호스트의 상태 변경을 DB에 반영하고 구독자들에게 브로드캐스팅
     */
    @MessageMapping("/party/{partyId}/extension/state")
    @Transactional
    public void handleStateSync(@DestinationVariable Integer partyId, @Payload StateSyncDto stateSyncDto,
                                Principal principal) {
        if (principal == null) {
            log.warn("Unauthorized attempt to sync state for partyId: {}", partyId);
            return;
        }

        User user = getUserByPrincipal(principal);
        Participant participant = getParticipant(partyId, user.getId());

        // 권한 검증: HOST (또는 MANAGER) 만 상태 변경 가능
        if (participant.getRole() == ParticipantRole.GUEST) {
            log.warn("Guest attempted to change state. User: {}, Party: {}", user.getId(), partyId);
            // 에러 메시지를 보낼 수도 있지만, 여기서는 무시하거나 로그만 남김 (보안상 조용히 처리)
            return;
        }

        // DB 업데이트 (SSOT)
        PartyState partyState = partyStateRepository.findById(partyId)
                .orElse(null);

        if (partyState == null) {
            // 없으면 생성
            Party party = partyRepository.findById(partyId)
                    .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTY_NOT_FOUND));

            partyState = PartyState.builder()
                    .party(party)
                    .partyUrl(stateSyncDto.getPartyUrl())
                    .lastCommand(stateSyncDto.getCommandType())
                    .currentPosition((int) Math.round(stateSyncDto.getCurrentPosition()))
                    .isPlaying(stateSyncDto.getIsPlaying())
                    .updatedAt(LocalDateTime.now())
                    .build();
            partyStateRepository.save(partyState);
        } else {
            // 있으면 업데이트 (Dirty Checking)
            partyState.update(
                    stateSyncDto.getPartyUrl(),
                    stateSyncDto.getCommandType(),
                    (int) Math.round(stateSyncDto.getCurrentPosition()),
                    stateSyncDto.getIsPlaying());
        }

        // 브로드캐스팅
        messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", stateSyncDto);
        log.info("Broadcasted state for party {}: {}", partyId, stateSyncDto.getCommandType());
    }

    private User getUserByPrincipal(Principal principal) {
        String email = principal.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
    }

    private Participant getParticipant(Integer partyId, Integer userId) {
        return participantRepository.findByPartyIdAndUserId(partyId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTICIPANT_NOT_FOUND));
    }
}
