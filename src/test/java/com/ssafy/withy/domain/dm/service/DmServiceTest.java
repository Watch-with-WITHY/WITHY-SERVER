package com.ssafy.withy.domain.dm.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DmService 테스트")
class DmServiceTest {
    
    @Mock
    private DmRoomRepository dmRoomRepository;
    
    @Mock
    private DmMessageRepository dmMessageRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private DmService dmService;

    @Test
    @DisplayName("DM 메시지 전송 성공")
    void sendMessage_Success() {
        // given
        Integer roomId = 1;
        Integer senderId = 1;
        String messageContent = "새로운 메시지";
        
        com.ssafy.withy.domain.dm.dto.DmMessageRequest request = new com.ssafy.withy.domain.dm.dto.DmMessageRequest(roomId, messageContent);
        
        User sender = User.builder().id(senderId).nickname("나").build();
        User receiver = User.builder().id(2).nickname("상대방").build();
        
        DmRoom room = DmRoom.builder()
                .id(roomId)
                .userA(sender)
                .userB(receiver)
                .build();
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
        
        // when
        dmService.sendMessage(senderId, request);
        
        // then
        // 1. 메시지 저장 확인
        verify(dmMessageRepository, times(1)).save(any(DmMessage.class));
        
        // 2. 방 정보 업데이트 확인 (LastMessage)
        assertThat(room.getLastMessage()).isEqualTo(messageContent);
        assertThat(room.getLastMessageAt()).isNotNull();
        
        // 3. 브로드캐스팅 확인
        verify(messagingTemplate, times(1)).convertAndSend(eq("/sub/dm/room/" + roomId), any(DmMessageResponse.class));
    }
    
    @Test
    @DisplayName("DM 메시지 전송 실패 - 존재하지 않는 방")
    void sendMessage_Fail_RoomNotFound() {
        // given
        Integer roomId = 999;
        Integer senderId = 1;
        com.ssafy.withy.domain.dm.dto.DmMessageRequest request = new com.ssafy.withy.domain.dm.dto.DmMessageRequest(roomId, "메시지");
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> dmService.sendMessage(senderId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.DM_ROOM_NOT_FOUND);
    }
    
    @Test
    @DisplayName("DM 메시지 전송 실패 - 권한 없음")
    void sendMessage_Fail_AccessDenied() {
        // given
        Integer roomId = 1;
        Integer senderId = 3; // 방 참여자가 아님
        com.ssafy.withy.domain.dm.dto.DmMessageRequest request = new com.ssafy.withy.domain.dm.dto.DmMessageRequest(roomId, "메시지");
        
        User user1 = User.builder().id(1).build();
        User user2 = User.builder().id(2).build();
        
        DmRoom room = DmRoom.builder()
                .id(roomId)
                .userA(user1)
                .userB(user2)
                .build();
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        
        // when & then
        assertThatThrownBy(() -> dmService.sendMessage(senderId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.DM_ROOM_ACCESS_DENIED);
    }

    
    @Test
    @DisplayName("DM 방 생성 성공 - 새 방 생성")
    void createRoom_Success_NewRoom() {
        // given
        Integer currentUserId = 1;
        Integer targetUserId = 2;
        
        User currentUser = User.builder().id(currentUserId).nickname("사용자1").build();
        User targetUser = User.builder().id(targetUserId).nickname("사용자2").build();
        
        DmRoom newRoom = DmRoom.builder()
                .id(1)
                .userA(currentUser)
                .userB(targetUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
        given(dmRoomRepository.findByTwoUsers(currentUserId, targetUserId)).willReturn(Optional.empty());
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
        given(dmRoomRepository.save(any(DmRoom.class))).willReturn(newRoom);
        
        // when
        DmRoomResponse response = dmService.createRoom(currentUserId, targetUserId);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo(1);
        assertThat(response.getTargetUser().getUserId()).isEqualTo(targetUserId);
        assertThat(response.getTargetUser().getNickname()).isEqualTo("사용자2");
        
        verify(dmRoomRepository, times(1)).save(any(DmRoom.class));
    }
    
    @Test
    @DisplayName("DM 방 생성 성공 - 기존 방 조회")
    void createRoom_Success_ExistingRoom() {
        // given
        Integer currentUserId = 1;
        Integer targetUserId = 2;
        
        User currentUser = User.builder().id(currentUserId).nickname("사용자1").build();
        User targetUser = User.builder().id(targetUserId).nickname("사용자2").build();
        
        DmRoom existingRoom = DmRoom.builder()
                .id(1)
                .userA(currentUser)
                .userB(targetUser)
                .lastMessage("안녕하세요")
                .lastMessageAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
        given(dmRoomRepository.findByTwoUsers(currentUserId, targetUserId)).willReturn(Optional.of(existingRoom));
        
        // when
        DmRoomResponse response = dmService.createRoom(currentUserId, targetUserId);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo(1);
        assertThat(response.getLastMessage()).isEqualTo("안녕하세요");
        
        verify(dmRoomRepository, never()).save(any(DmRoom.class));
    }
    
    @Test
    @DisplayName("DM 방 생성 실패 - 자기 자신과 DM 시도")
    void createRoom_Fail_SelfDm() {
        // given
        Integer userId = 1;
        
        // when & then
        assertThatThrownBy(() -> dmService.createRoom(userId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.DM_SELF_NOT_ALLOWED);
    }
    
    @Test
    @DisplayName("DM 방 생성 실패 - 존재하지 않는 사용자")
    void createRoom_Fail_UserNotFound() {
        // given
        Integer currentUserId = 1;
        Integer targetUserId = 999;
        
        given(userRepository.findById(targetUserId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> dmService.createRoom(currentUserId, targetUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.USER_NOT_FOUND);
    }
    
    @Test
    @DisplayName("상대방 ID로 DM 방 조회 성공")
    void getRoomByTargetIds_Success() {
        // given
        Integer currentUserId = 1;
        Integer targetUserId = 2;
        
        User currentUser = User.builder().id(currentUserId).build();
        User targetUser = User.builder().id(targetUserId).build();
        
        DmRoom room = DmRoom.builder()
                .id(1)
                .userA(currentUser)
                .userB(targetUser)
                .build();
        
        given(dmRoomRepository.findByTwoUsers(currentUserId, targetUserId)).willReturn(Optional.of(room));
        
        // when
        DmRoomResponse response = dmService.getRoomByTargetIds(currentUserId, targetUserId);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo(1);
        assertThat(response.getIsLeft()).isFalse(); // 참여 중이므로 false
    }
    
    @Test
    @DisplayName("상대방 ID로 DM 방 조회 실패 - 방 없음")
    void getRoomByTargetIds_Fail_NotFound() {
        // given
        Integer currentUserId = 1;
        Integer targetUserId = 2;
        
        given(dmRoomRepository.findByTwoUsers(currentUserId, targetUserId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> dmService.getRoomByTargetIds(currentUserId, targetUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.DM_ROOM_NOT_FOUND);
    }
    
    @Test
    @DisplayName("상대방 ID로 DM 방 조회 성공 - 방은 있지만 나간 상태 (IsLeft=true 반환)")
    void getRoomByTargetIds_Success_LeftRoom() {
        // given
        Integer currentUserId = 1;
        Integer targetUserId = 2;
        
        User currentUser = User.builder().id(currentUserId).build();
        User targetUser = User.builder().id(targetUserId).build();
        
        DmRoom room = DmRoom.builder()
                .id(1)
                .userA(currentUser)
                .userB(targetUser)
                .build();
        
        room.leave(currentUserId); // 나감 처리
        
        given(dmRoomRepository.findByTwoUsers(currentUserId, targetUserId)).willReturn(Optional.of(room));
        
        // when
        DmRoomResponse response = dmService.getRoomByTargetIds(currentUserId, targetUserId); // 이제 예외가 아니라 성공해야 함

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo(1);
        assertThat(response.getIsLeft()).isTrue(); // 나간 상태 확인
    }

    @Test
    @DisplayName("DM 메시지 조회 성공 - 나간 상태(IsLeft=true)면 빈 리스트 반환")
    void getMessages_Success_LeftRoom_ReturnsEmpty() {
        // given
        Integer roomId = 1;
        Integer currentUserId = 1;
        Pageable pageable = PageRequest.of(0, 20);

        User currentUser = User.builder().id(currentUserId).build();
        User targetUser = User.builder().id(2).build();

        DmRoom room = DmRoom.builder()
                .id(roomId)
                .userA(currentUser)
                .userB(targetUser)
                .build();
        
        room.leave(currentUserId); // 나감 상태

        given(dmRoomRepository.findById(roomId)).willReturn(Optional.of(room));

        // when
        Page<DmMessageResponse> responses = dmService.getMessages(roomId, currentUserId, pageable);

        // then
        assertThat(responses).isEmpty(); // 빈 페이지 반환 확인
        verify(dmMessageRepository, never()).findByRoom_IdOrderByCreatedAtDesc(any(), any()); // DB 조회 안함 확인
    }
    
    @Test
    @DisplayName("내 DM 방 목록 조회 성공")
    void getMyRooms_Success() {
        // given
        Integer currentUserId = 1;
        
        User currentUser = User.builder().id(currentUserId).nickname("나").build();
        User user2 = User.builder().id(2).nickname("사용자2").build();
        User user3 = User.builder().id(3).nickname("사용자3").build();
        
        DmRoom room1 = DmRoom.builder()
                .id(1)
                .userA(currentUser)
                .userB(user2)
                .lastMessage("최근 메시지")
                .lastMessageAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        
        DmRoom room2 = DmRoom.builder()
                .id(2)
                .userA(user3)
                .userB(currentUser)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        
        given(dmRoomRepository.findAllByUserId(currentUserId)).willReturn(Arrays.asList(room1, room2));
        
        // when
        List<DmRoomResponse> responses = dmService.getMyRooms(currentUserId);
        
        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getRoomId()).isEqualTo(1);
        assertThat(responses.get(0).getTargetUser().getNickname()).isEqualTo("사용자2");
        assertThat(responses.get(1).getRoomId()).isEqualTo(2);
        assertThat(responses.get(1).getTargetUser().getNickname()).isEqualTo("사용자3");
    }
    
    @Test
    @DisplayName("DM 메시지 조회 성공")
    void getMessages_Success() {
        // given
        Integer roomId = 1;
        Integer currentUserId = 1;
        Pageable pageable = PageRequest.of(0, 20);
        
        User currentUser = User.builder().id(currentUserId).nickname("나").build();
        User targetUser = User.builder().id(2).nickname("상대방").build();
        
        DmRoom room = DmRoom.builder()
                .id(roomId)
                .userA(currentUser)
                .userB(targetUser)
                .build();
        
        DmMessage message1 = DmMessage.builder()
                .id(1)
                .room(room)
                .sender(currentUser)
                .message("안녕하세요")
                .createdAt(LocalDateTime.now())
                .build();
        
        DmMessage message2 = DmMessage.builder()
                .id(2)
                .room(room)
                .sender(targetUser)
                .message("반갑습니다")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();
        
        Page<DmMessage> messagePage = new PageImpl<>(Arrays.asList(message1, message2), pageable, 2);
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(dmMessageRepository.findByRoom_IdOrderByCreatedAtDesc(roomId, pageable)).willReturn(messagePage);
        
        // when
        Page<DmMessageResponse> responses = dmService.getMessages(roomId, currentUserId, pageable);
        
        // then
        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent().get(0).getMessage()).isEqualTo("안녕하세요");
        assertThat(responses.getContent().get(0).getSenderNickname()).isEqualTo("나");
        assertThat(responses.getContent().get(1).getMessage()).isEqualTo("반갑습니다");
        assertThat(responses.getContent().get(1).getSenderNickname()).isEqualTo("상대방");
        assertThat(responses.getTotalElements()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("DM 메시지 조회 실패 - 존재하지 않는 방")
    void getMessages_Fail_RoomNotFound() {
        // given
        Integer roomId = 999;
        Integer currentUserId = 1;
        Pageable pageable = PageRequest.of(0, 20);
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> dmService.getMessages(roomId, currentUserId, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.DM_ROOM_NOT_FOUND);
    }
    
    @Test
    @DisplayName("DM 메시지 조회 실패 - 권한 없음")
    void getMessages_Fail_AccessDenied() {
        // given
        Integer roomId = 1;
        Integer currentUserId = 3; // 방 참여자가 아님
        Pageable pageable = PageRequest.of(0, 20);
        
        User user1 = User.builder().id(1).nickname("사용자1").build();
        User user2 = User.builder().id(2).nickname("사용자2").build();
        
        DmRoom room = DmRoom.builder()
                .id(roomId)
                .userA(user1)
                .userB(user2)
                .build();
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        
        // when & then
        assertThatThrownBy(() -> dmService.getMessages(roomId, currentUserId, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.DM_ROOM_ACCESS_DENIED);
    }
    
    @Test
    @DisplayName("DM 방 나가기 성공 - Soft Delete (상대방은 남아있음)")
    void deleteRoom_Success_SoftDelete() {
        // given
        Integer roomId = 1;
        Integer userId = 1;
        
        User user1 = User.builder().id(1).build();
        User user2 = User.builder().id(2).build();
        
        DmRoom room = DmRoom.builder()
                .id(roomId)
                .userA(user1)
                .userB(user2)
                .build();
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        
        // when
        dmService.deleteRoom(roomId, userId);
        
        // then
        assertThat(room.isLeft(userId)).isTrue();
        assertThat(room.isLeft(2)).isFalse();
        verify(dmRoomRepository, never()).delete(any(DmRoom.class));
    }
    
    @Test
    @DisplayName("DM 방 나가기 성공 - Hard Delete (모두 나감)")
    void deleteRoom_Success_HardDelete() {
        // given
        Integer roomId = 1;
        Integer userId = 1;
        
        User user1 = User.builder().id(1).build();
        User user2 = User.builder().id(2).build();
        
        DmRoom room = DmRoom.builder()
                .id(roomId)
                .userA(user1)
                .userB(user2)
                .build();
        
        // 상대방은 이미 나간 상태
        room.leave(2);
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        
        // when
        dmService.deleteRoom(roomId, userId);
        
        // then
        assertThat(room.isLeft(userId)).isTrue();
        verify(dmRoomRepository, times(1)).delete(room);
    }
    
    @Test
    @DisplayName("DM 방 나가기 실패 - 존재하지 않는 방")
    void deleteRoom_Fail_NotFound() {
        // given
        Integer roomId = 999;
        Integer userId = 1;
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> dmService.deleteRoom(roomId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.DM_ROOM_NOT_FOUND);
    }
    
    @Test
    @DisplayName("DM 방 나가기 실패 - 권한 없음")
    void deleteRoom_Fail_AccessDenied() {
        // given
        Integer roomId = 1;
        Integer userId = 3; // 참여자 아님
        
        User user1 = User.builder().id(1).build();
        User user2 = User.builder().id(2).build();
        
        DmRoom room = DmRoom.builder()
                .id(roomId)
                .userA(user1)
                .userB(user2)
                .build();
        
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        
        // when & then
        assertThatThrownBy(() -> dmService.deleteRoom(roomId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.DM_ROOM_ACCESS_DENIED);
    }
    
    @Test
    @DisplayName("DM 방 재생성/조회 시 나간 방 복구 (Rejoin) 및 JoinedAt 갱신")
    void createRoom_Success_Rejoin() {
        // given
        Integer currentUserId = 1;
        Integer targetUserId = 2;
        
        User currentUser = User.builder().id(currentUserId).build();
        User targetUser = User.builder().id(targetUserId).build();
        
        DmRoom room = DmRoom.builder()
                .id(1)
                .userA(currentUser)
                .userB(targetUser)
                .build();
        
        // 이미 나간 상태로 설정
        room.leave(currentUserId);
        
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
        given(dmRoomRepository.findByTwoUsers(currentUserId, targetUserId)).willReturn(Optional.of(room));
        
        // when
        dmService.createRoom(currentUserId, targetUserId);
        
        // then
        assertThat(room.isLeft(currentUserId)).isFalse(); // 복구 확인
        assertThat(room.getJoinedAt(currentUserId)).isNotNull(); // JoinedAt 갱신 확인
    }

    @Test
    @DisplayName("DM 메시지 조회 성공 - History Filtering (나갔다 들어온 후)")
    void getMessages_Success_HistoryFiltering() {
        // given
        Integer roomId = 1;
        Integer currentUserId = 1;
        Pageable pageable = PageRequest.of(0, 20);
        
        User currentUser = User.builder().id(currentUserId).nickname("나").build();
        User targetUser = User.builder().id(2).nickname("상대방").build();
        
        DmRoom room = DmRoom.builder()
                .id(roomId)
                .userA(currentUser)
                .userB(targetUser)
                .build();
        
        // JoinedAt 설정 (최근 재입장)
        LocalDateTime rejoinedAt = LocalDateTime.now();
        room.rejoin(currentUserId, rejoinedAt);
        
        // joinedAt 이후의 메시지만 조회되어야 함
        given(dmRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(dmMessageRepository.findByRoom_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                eq(roomId), eq(rejoinedAt), eq(pageable)))
                .willReturn(new PageImpl<>(List.of())); // Mock return empty for simplicity
        
        // when
        dmService.getMessages(roomId, currentUserId, pageable);
        
        // then
        verify(dmMessageRepository, times(1))
                .findByRoom_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(roomId), eq(rejoinedAt), eq(pageable));
        verify(dmMessageRepository, never())
                .findByRoom_IdOrderByCreatedAtDesc(any(), any());
    }
}
