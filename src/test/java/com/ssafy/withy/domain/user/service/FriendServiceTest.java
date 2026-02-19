package com.ssafy.withy.domain.user.service;

import com.ssafy.withy.domain.user.dto.FriendDto;
import com.ssafy.withy.domain.user.dto.FriendRequestResponse;
import com.ssafy.withy.domain.user.dto.UserSearchResponse;
import com.ssafy.withy.domain.user.entity.FriendRequest;
import com.ssafy.withy.domain.user.entity.FriendRequestStatus;
import com.ssafy.withy.domain.user.entity.Friendship;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.entity.UserStatus;
import com.ssafy.withy.domain.user.repository.FriendRequestRepository;
import com.ssafy.withy.domain.user.repository.FriendshipRepository;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @InjectMocks
    private FriendService friendService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Test
    @DisplayName("친구 신청 발송 성공")
    void sendFriendRequest_Success() {
        // given
        Integer requesterId = 1;
        Integer receiverId = 2;
        User requester = User.builder().id(requesterId).build();
        User receiver = User.builder().id(receiverId).build();

        given(userRepository.findById(requesterId)).willReturn(Optional.of(requester));
        given(userRepository.findById(receiverId)).willReturn(Optional.of(receiver));
        given(friendshipRepository.existsFriendship(requester, receiver)).willReturn(false);
        given(friendRequestRepository.existsByRequesterAndReceiverAndStatus(requester, receiver,
                FriendRequestStatus.PENDING)).willReturn(false);
        given(friendRequestRepository.existsByRequesterAndReceiverAndStatus(receiver, requester,
                FriendRequestStatus.PENDING)).willReturn(false);

        // when
        friendService.sendFriendRequest(requesterId, receiverId);

        // then
        verify(friendRequestRepository).save(any(FriendRequest.class));
    }

    @Test
    @DisplayName("친구 신청 발송 실패 - 이미 친구")
    void sendFriendRequest_Fail_AlreadyFriend() {
        // given
        Integer requesterId = 1;
        Integer receiverId = 2;
        User requester = User.builder().id(requesterId).build();
        User receiver = User.builder().id(receiverId).build();

        given(userRepository.findById(requesterId)).willReturn(Optional.of(requester));
        given(userRepository.findById(receiverId)).willReturn(Optional.of(receiver));
        given(friendshipRepository.existsFriendship(requester, receiver)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> friendService.sendFriendRequest(requesterId, receiverId))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 친구 관계입니다.");
    }

    @Test
    @DisplayName("친구 신청 수락 성공")
    void handleFriendRequest_Accept() {
        // given
        Integer userId = 2;
        Integer requestId = 10;
        User requester = User.builder().id(1).build();
        User receiver = User.builder().id(userId).build();
        FriendRequest request = FriendRequest.builder()
                .id(requestId)
                .requester(requester)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when
        friendService.handleFriendRequest(userId, requestId, true);

        // then
        verify(friendshipRepository).save(any(Friendship.class));
        verify(friendRequestRepository).delete(request);
    }

    @Test
    @DisplayName("친구 신청 거절 성공")
    void handleFriendRequest_Deny() {
        // given
        Integer userId = 2;
        Integer requestId = 10;
        User requester = User.builder().id(1).build();
        User receiver = User.builder().id(userId).build();
        FriendRequest request = FriendRequest.builder()
                .id(requestId)
                .requester(requester)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when
        friendService.handleFriendRequest(userId, requestId, false);

        // then
        // Friendship 저장은 호출되지 않아야 함
        verify(friendRequestRepository).delete(request);
    }

    @Test
    @DisplayName("받은 친구 신청 목록 조회")
    void getReceivedFriendRequests() {
        // given
        Integer userId = 2;
        User receiver = User.builder().id(userId).build();
        User requester = User.builder().id(1).nickname("req").profileImageUrl("img").build();
        FriendRequest request = FriendRequest.builder()
                .id(10)
                .requester(requester)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByReceiverAndStatus(receiver, FriendRequestStatus.PENDING))
                .willReturn(List.of(request));

        // when
        List<FriendRequestResponse> responses = friendService.getReceivedFriendRequests(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).requesterNickname()).isEqualTo("req");
    }

    @Test
    @DisplayName("친구 목록 조회")
    void getFriendList() {
        // given
        Integer userId = 1;
        User user = User.builder().id(userId).build();
        User friend = User.builder().id(2).nickname("friend").profileImageUrl("img").status(UserStatus.ONLINE).build();

        // Friendship에서 userA가 나, userB가 친구
        Friendship friendship = Friendship.builder().userA(user).userB(friend).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(friendshipRepository.findAllByUserAOrUserB(user, user)).willReturn(List.of(friendship));

        // when
        List<FriendDto> friends = friendService.getFriendList(userId);

        // then
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).status()).isEqualTo(UserStatus.ONLINE);
        assertThat(friends.get(0).nickname()).isEqualTo("friend");
    }

    @Test
    @DisplayName("친구 삭제 성공")
    void deleteFriend_Success() {
        // given
        Integer userId = 1;
        Integer friendId = 2;
        User user = User.builder().id(userId).build();
        User friend = User.builder().id(friendId).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.findById(friendId)).willReturn(Optional.of(friend));
        given(friendshipRepository.existsFriendship(user, friend)).willReturn(true);

        // when
        friendService.deleteFriend(userId, friendId);

        // then
        verify(friendshipRepository).deleteFriendship(user, friend);
    }

    @Nested
    @DisplayName("보낸 친구 신청 목록 조회")
    class GetSentRequestsTest {

        @Test
        @DisplayName("성공: 내가 보낸 대기 중(PENDING)인 요청 목록을 조회한다.")
        void getSentRequests_Success() {
            // given
            Integer myId = 1;
            User me = createUser(myId, "나");
            User target = createUser(2, "상대방");

            FriendRequest request = FriendRequest.builder()
                    .id(10)
                    .requester(me)
                    .receiver(target)
                    .status(FriendRequestStatus.PENDING)
                    .build();

            given(friendRequestRepository.findAllSentRequests(myId))
                    .willReturn(List.of(request));

            // when
            List<FriendRequestResponse> result = friendService.getSentFriendRequests(myId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).requesterNickname()).isEqualTo("상대방"); // receiver 정보 확인
            assertThat(result.get(0).requestId()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("친구 신청 취소 (철회)")
    class CancelRequestTest {

        @Test
        @DisplayName("성공: 본인이 보낸 PENDING 상태의 요청은 취소(삭제)할 수 있다.")
        void cancel_Success() {
            // given
            Integer myId = 1;
            Integer requestId = 10;
            User me = createUser(myId, "나");
            User target = createUser(2, "상대방");

            FriendRequest request = FriendRequest.builder()
                    .id(requestId)
                    .requester(me) // 보낸 사람 = 나
                    .receiver(target)
                    .status(FriendRequestStatus.PENDING) // 상태 = PENDING
                    .build();

            given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            // when
            friendService.cancelFriendRequest(myId, requestId);

            // then
            then(friendRequestRepository).should(times(1)).delete(request);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 요청 ID면 예외가 발생한다.")
        void cancel_Fail_NotFound() {
            // given
            given(friendRequestRepository.findById(anyInt())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> friendService.cancelFriendRequest(1, 999))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 내가 보낸 요청이 아니면 취소할 수 없다.")
        void cancel_Fail_NotSender() {
            // given
            Integer myId = 1;
            Integer otherId = 2; // 다른 사람
            User other = createUser(otherId, "남");

            FriendRequest request = FriendRequest.builder()
                    .requester(other) // 보낸 사람이 "남"임
                    .build();

            given(friendRequestRepository.findById(anyInt())).willReturn(Optional.of(request));

            // when & then (내가 취소 시도)
            assertThatThrownBy(() -> friendService.cancelFriendRequest(myId, 10))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.NOT_REQUESTER);
        }

        @Test
        @DisplayName("실패: 이미 처리된(수락/거절) 요청은 취소할 수 없다.")
        void cancel_Fail_NotPending() {
            // given
            Integer myId = 1;
            User me = createUser(myId, "나");

            FriendRequest request = FriendRequest.builder()
                    .requester(me)
                    .status(FriendRequestStatus.ACCEPTED) // 이미 수락됨
                    .build();

            given(friendRequestRepository.findById(anyInt())).willReturn(Optional.of(request));

            // when & then
            assertThatThrownBy(() -> friendService.cancelFriendRequest(myId, 10))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.CANNOT_CANCEL_PROCESSED_REQUEST);
        }
    }

    // Helper Method
    private User createUser(Integer id, String nickname) {
        return User.builder()
                .id(id)
                .nickname(nickname)
                .profileImageUrl("img.jpg")
                .build();
    }
}
