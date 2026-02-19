package com.ssafy.withy.domain.dm.service;

import com.ssafy.withy.domain.dm.dto.DmMessageRequest;
import com.ssafy.withy.domain.dm.dto.DmMessageResponse;
import com.ssafy.withy.domain.dm.dto.DmRoomResponse;
import com.ssafy.withy.domain.dm.entity.DmMessage;
import com.ssafy.withy.domain.dm.entity.DmRoom;
import com.ssafy.withy.domain.dm.repository.DmMessageRepository;
import com.ssafy.withy.domain.dm.repository.DmRoomRepository;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DmService {

    private final DmRoomRepository dmRoomRepository;
    private final DmMessageRepository dmMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * DM 방 생성 또는 조회
     * DM 방 생성 (또는 기존 방 반환)
     * - 메시지 전송 시점에 호출됨
     * - 없으면 생성, 있으면 반환 (Idempotent)
     * - 나갔던 방이면 복구 (Rejoin)
     */
    @Transactional
    public DmRoomResponse createRoom(Integer currentUserId, Integer targetUserId) {
        // 1. 자기 자신과 DM 불가
        if (currentUserId.equals(targetUserId)) {
            throw new CustomException(GlobalErrorCode.DM_SELF_NOT_ALLOWED);
        }

        // 2. 상대방 존재 확인
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 3. 기존 DM 방 조회 (순서 무관)
        DmRoom room = dmRoomRepository.findByTwoUsers(currentUserId, targetUserId)
                .orElseGet(() -> {
                    // 4. 없으면 새 방 생성
                    User currentUser = userRepository.findById(currentUserId)
                            .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

                    DmRoom newRoom = DmRoom.builder()
                            .userA(currentUser)
                            .userB(targetUser)
                            .build();

                    return dmRoomRepository.save(newRoom);
                });

        // 5. 나갔던 방이면 복구 (Rejoin)
        if (room.isLeft(currentUserId)) {
            // 방금 다시 들어왔으므로 현재 시점으로 JoinedAt 갱신 (이전 기록 숨김)
            room.rejoin(currentUserId, LocalDateTime.now());
        }

        // 6. Response 변환 (상대방 정보만 포함)
        return DmRoomResponse.from(room, currentUserId);
    }

    /**
     * 상대방과의 DM 방 존재 여부 조회
     * - 친구 목록 클릭 시 호출
     * - 방이 없으면 404 Not Found
     * - 방이 있지만 나간 상태여도 방 정보 반환 (isLeft=true) -> 프론트에서 재입장 지원
     */
    @Transactional(readOnly = true)
    public DmRoomResponse getRoomByTargetIds(Integer currentUserId, Integer targetUserId) {
        DmRoom room = dmRoomRepository.findByTwoUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.DM_ROOM_NOT_FOUND));

        // [수정] 나간 상태라도 404를 던지지 않고, 방 정보를 반환함.
        // DTO의 isLeft 필드를 통해 프론트엔드가 상태를 파악함.
        return DmRoomResponse.from(room, currentUserId);
    }

    /**
     * 내가 참여 중인 모든 DM 방 목록 조회
     * - 최근 메시지가 있는 방부터 정렬
     * 
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return List<DmRoomResponse>
     */
    @Transactional(readOnly = true)
    public List<DmRoomResponse> getMyRooms(Integer currentUserId) {
        // Repository에서 이미 나간 방은 제외하고 조회함
        List<DmRoom> rooms = dmRoomRepository.findAllByUserId(currentUserId);

        return rooms.stream()
                .map(room -> DmRoomResponse.from(room, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 특정 DM 방의 메시지 목록 조회 (페이지네이션)
     * - 최신 메시지부터 내림차순 정렬
     * - 권한 확인: 해당 방의 참여자만 조회 가능
     * - History Filtering: 나간 시점 이전의 메시지는 보여주지 않음 (JoinedAt 이후만 조회)
     */
    @Transactional(readOnly = true)
    public Page<DmMessageResponse> getMessages(Integer roomId, Integer currentUserId, Pageable pageable) {
        // 1. DM 방 조회
        DmRoom room = dmRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.DM_ROOM_NOT_FOUND));

        // 2. 권한 확인
        boolean isParticipant = room.getUserA().getId().equals(currentUserId)
                || room.getUserB().getId().equals(currentUserId);

        if (!isParticipant) {
            throw new CustomException(GlobalErrorCode.DM_ROOM_ACCESS_DENIED);
        }

        // [추가] 사용자가 현재 '나간 상태(isLeft)'라면 메시지를 보여주지 않음 (빈 화면)
        if (room.isLeft(currentUserId)) {
            return Page.empty(pageable);
        }

        // 3. 메시지 조회 (JoinedAt 이후의 메시지만 필터링)
        LocalDateTime joinedAt = room.getJoinedAt(currentUserId);

        Page<DmMessage> messages;
        if (joinedAt != null) {
            messages = dmMessageRepository.findByRoom_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                    roomId, joinedAt, pageable);
        } else {
            // 하위 호환성 (JoinedAt이 없는 경우 전체 조회)
            messages = dmMessageRepository.findByRoom_IdOrderByCreatedAtDesc(roomId, pageable);
        }

        // 4. Response 변환
        return messages.map(DmMessageResponse::from);
    }

    /**
     * DM 메시지 전송 (WebSocket)
     */
    @Transactional
    public void sendMessage(Integer senderId, DmMessageRequest request) {
        // 1. 방 존재 확인
        DmRoom room = dmRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.DM_ROOM_NOT_FOUND));

        // 2. 권한 확인 (참여자 여부)
        boolean isParticipant = room.getUserA().getId().equals(senderId)
                || room.getUserB().getId().equals(senderId);
        if (!isParticipant) {
            throw new CustomException(GlobalErrorCode.DM_ROOM_ACCESS_DENIED);
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 3. 메시지 저장
        // 메시지 생성 시간을 미리 정의하여 Rejoin과 동기화
        LocalDateTime messageTime = LocalDateTime.now();

        DmMessage dmMessage = DmMessage.builder()
                .room(room)
                .sender(sender)
                .message(request.getMessage())
                // .createdAt(messageTime) // Auditing이 자동으로 해주지만, 명시적으로 사용할 수도 있음. 일단 자동 맡김.
                .build();
        dmMessageRepository.save(dmMessage);

        // 4. 방 정보 업데이트 (Last Message) 및 방 복구 (양쪽 모두에게 보이기)
        room.updateLastMessage(request.getMessage(), messageTime);

        // 메시지가 오면 나간 사람도 다시 방이 보여야 함 (Rejoin)
        // 이때 JoinedAt을 갱신하는데, "이 메시지부터" 보여야 하므로 메시지 생성 시간(혹은 그보다 살짝 전)을 기준으로 함.
        // Auditing에 의해 저장된 시간과 약간의 오차가 있을 수 있으므로, 안전하게 1초 전으로 하거나 그냥 현재 시간 사용.
        // 여기서는 messageTime 사용. (JPA Auditing 시간과 거의 차이 없음)

        if (room.isLeft(room.getUserA().getId())) {
            room.rejoin(room.getUserA().getId(), messageTime);
        }
        if (room.isLeft(room.getUserB().getId())) {
            room.rejoin(room.getUserB().getId(), messageTime);
        }

        // 5. 실시간 브로드캐스팅
        // 구독 경로: /sub/dm/room/{roomId}
        // 응답 포맷: DmMessageResponse (기존 DTO 재활용)
        DmMessageResponse response = DmMessageResponse.from(dmMessage);
        messagingTemplate.convertAndSend("/sub/dm/room/" + room.getId(), response);

        // [추가] 상대방에게 알림 전송 (Global Notification)
        // 구독 경로: /sub/user/{userId}/dm
        Integer recipientId = room.getUserA().getId().equals(senderId)
                ? room.getUserB().getId()
                : room.getUserA().getId();
        messagingTemplate.convertAndSend("/sub/user/" + recipientId + "/dm", response);
    }

    /**
     * DM 방 나가기 (삭제)
     * - 해당 유저의 LeftAt 갱신
     * - 양쪽 모두 나가면 Hard Delete
     */
    @Transactional
    public void deleteRoom(Integer roomId, Integer userId) {
        DmRoom room = dmRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.DM_ROOM_NOT_FOUND));

        // 권한 확인
        if (!room.getUserA().getId().equals(userId) && !room.getUserB().getId().equals(userId)) {
            throw new CustomException(GlobalErrorCode.DM_ROOM_ACCESS_DENIED);
        }

        // 나가기 처리
        room.leave(userId);

        // 양쪽 모두 나갔는지 확인
        boolean userALeft = room.isLeft(room.getUserA().getId());
        boolean userBLeft = room.isLeft(room.getUserB().getId());

        if (userALeft && userBLeft) {
            dmRoomRepository.delete(room);
        }
    }
}
