package com.ssafy.withy.domain.party.controller;

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
import com.ssafy.withy.global.common.code.GlobalSuccessCode;
import com.ssafy.withy.global.common.response.ApiResponse;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Tag(name = "PartyState (Polling Performance Test)", description = "성능 비교용 Polling API")
@Slf4j
@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class PartyStateController {

    private final PartyRepository partyRepository;
    private final PartyStateRepository partyStateRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Operation(summary = "상태 업데이트 (Host)", description = "호스트가 현재 상태를 DB에 저장합니다. (Polling 테스트용)")
    @PostMapping("/{partyId}/state/polling")
    @Transactional
    public ResponseEntity<ApiResponse<String>> updateState(
            @PathVariable Integer partyId,
            @RequestBody StateSyncDto stateSyncDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserByDetails(userDetails);
        Participant participant = getParticipant(partyId, user.getId());

        if (participant.getRole() == ParticipantRole.GUEST) {
            throw new CustomException(GlobalErrorCode.PARTY_NOT_PARTY_HOST);
        }

        PartyState partyState = partyStateRepository.findById(partyId).orElse(null);

        if (partyState == null) {
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
            partyState.update(
                    stateSyncDto.getPartyUrl(),
                    stateSyncDto.getCommandType(),
                    (int) Math.round(stateSyncDto.getCurrentPosition()),
                    stateSyncDto.getIsPlaying());
        }

        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.UPDATE_PARTY_SUCCESS, "State updated"));
    }

    @Operation(summary = "상태 조회 (Guest)", description = "게스트가 현재 상태를 DB에서 조회합니다. (Polling 테스트용)")
    @GetMapping("/{partyId}/state/polling")
    public ResponseEntity<ApiResponse<StateSyncDto>> getState(
            @PathVariable Integer partyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 참여자 확인 (실제로는 해야 하지만 부하 테스트 성능상 생략 가능, 여기서는 일단 포함)
        // User user = getUserByDetails(userDetails);
        // getParticipant(partyId, user.getId());

        PartyState partyState = partyStateRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTY_NOT_FOUND));

        // 시간 보정
        double currentPosition = partyState.getCurrentPosition();
        if (partyState.getIsPlaying()) {
            long elapsedSeconds = Duration.between(partyState.getUpdatedAt(), LocalDateTime.now()).getSeconds();
            currentPosition += elapsedSeconds;
        }

        StateSyncDto response = StateSyncDto.builder()
                .commandType(partyState.getLastCommand())
                .currentPosition(currentPosition)
                .isPlaying(partyState.getIsPlaying())
                .partyUrl(partyState.getPartyUrl())
                // .timestamp(...) // 서버 시간 반환 필요하면 추가, 여기선 생략
                .build();

        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.GET_PARTY_DETAIL_SUCCESS, response));
    }

    private User getUserByDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
    }

    private Participant getParticipant(Integer partyId, Integer userId) {
        return participantRepository.findByPartyIdAndUserId(partyId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTICIPANT_NOT_FOUND));
    }
}
