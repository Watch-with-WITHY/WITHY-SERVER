package com.ssafy.withy.domain.user.service;

import com.ssafy.withy.domain.user.dto.FriendDto;
import com.ssafy.withy.domain.user.dto.FriendRequestResponse;
import com.ssafy.withy.domain.user.entity.FriendRequest;
import com.ssafy.withy.domain.user.entity.FriendRequestStatus;
import com.ssafy.withy.domain.user.entity.Friendship;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.FriendRequestRepository;
import com.ssafy.withy.domain.user.repository.FriendshipRepository;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;

    @Transactional
    public void sendFriendRequest(Integer requesterId, Integer receiverId) {
        // 1. 본인에게 신청 불가
        if (requesterId.equals(receiverId)) {
            throw new CustomException(GlobalErrorCode.INVALID_REQUEST, "본인에게 친구 신청을 보낼 수 없습니다.");
        }

        // 2. 사용자 조회
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 3. 이미 친구 관계인지 확인
        if (friendshipRepository.existsFriendship(requester, receiver)) {
            throw new CustomException(GlobalErrorCode.INVALID_REQUEST, "이미 친구 관계입니다.");
        }

        // 4. 이미 신청 대기 중인지 확인 (내가 보낸 신청)
        if (friendRequestRepository.existsByRequesterAndReceiverAndStatus(requester, receiver,
                FriendRequestStatus.PENDING)) {
            throw new CustomException(GlobalErrorCode.INVALID_REQUEST, "이미 친구 신청을 보냈습니다.");
        }

        // 5. 이미 신청을 받은 상태인지 확인 (상대방이 나에게 보낸 신청) - 이 경우 바로 친구 수락을 할지, 아니면 중복이라고 알릴지 정책
        // 결정 필요.
        // 현재는 중복 신청 불가로 처리.
        if (friendRequestRepository.existsByRequesterAndReceiverAndStatus(receiver, requester,
                FriendRequestStatus.PENDING)) {
            throw new CustomException(GlobalErrorCode.INVALID_REQUEST, "상대방이 이미 친구 신청을 보냈습니다. 받은 요청을 확인해주세요.");
        }

        // 6. 친구 신청 저장
        FriendRequest request = FriendRequest.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();

        friendRequestRepository.save(request);
    }

    @Transactional
    public void handleFriendRequest(Integer userId, Integer requestId, Boolean isAccepted) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.ENTITY_NOT_FOUND, "존재하지 않는 친구 신청입니다."));

        // 유효성 검사: 요청 받은 당사자만 처리 가능
        if (!request.getReceiver().getId().equals(userId)) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED, "본인의 친구 신청만 처리할 수 있습니다.");
        }

        // 유효성 검사: 이미 처리(수락/거절)된 요청인지 확인 (PENDING 상태만 처리 가능)
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new CustomException(GlobalErrorCode.INVALID_REQUEST, "이미 처리된 친구 신청입니다.");
        }

        if (isAccepted) {
            // 친구 관계 생성
            Friendship friendship = Friendship.builder()
                    .userA(request.getRequester())
                    .userB(request.getReceiver())
                    .build();
            friendshipRepository.save(friendship);
        }

        // 연관관계 제거 (Cascade로 인한 좀비 데이터 방지)
        request.getRequester().getSentFriendRequests().removeIf(r -> r.getId().equals(requestId));
        request.getReceiver().getReceivedFriendRequests().removeIf(r -> r.getId().equals(requestId));

        // 요청 삭제 (수락이든 거절이든 요청은 삭제됨)
        friendRequestRepository.delete(request);
    }

    @Transactional
    public List<FriendRequestResponse> getReceivedFriendRequests(Integer userId) {
        User receiver = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        List<FriendRequest> requests = friendRequestRepository.findByReceiverAndStatus(receiver,
                FriendRequestStatus.PENDING);

        return requests.stream()
                .map(req -> new FriendRequestResponse(
                        req.getId(),
                        req.getRequester().getId(),
                        req.getRequester().getNickname(),
                        req.getRequester().getProfileImageUrl()))
                .toList();
    }

    @Transactional
    public List<FriendDto> getFriendList(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 내가 UserA일 수도 있고, UserB일 수도 있음
        List<Friendship> friendships = friendshipRepository.findAllByUserAOrUserB(user, user);

        return friendships.stream()
                .map(friendship -> {
                    User friend = friendship.getUserA().equals(user) ? friendship.getUserB() : friendship.getUserA();
                    return new FriendDto(
                            friend.getId(),
                            friend.getNickname(),
                            friend.getProfileImageUrl(),
                            friend.getStatus());
                })
                .toList();
    }

    @Transactional
    public void deleteFriend(Integer userId, Integer friendId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (!friendshipRepository.existsFriendship(user, friend)) {
            throw new CustomException(GlobalErrorCode.ENTITY_NOT_FOUND, "친구 관계가 존재하지 않습니다.");
        }

        friendshipRepository.deleteFriendship(user, friend);
    }

    // 친구 신청 보낸 목록 조회
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getSentFriendRequests(Integer userId) {
        // 1. 내가 보낸 PENDING 상태의 요청들 조회
        List<FriendRequest> requests = friendRequestRepository.findAllSentRequests(userId);

        // 2. DTO 변환 (받는 사람 정보 위주로)
        return requests.stream()
                .map(req -> FriendRequestResponse.builder()
                        .requestId(req.getId())
                        .requesterId(req.getReceiver().getId()) // 받는 사람 ID
                        .requesterNickname(req.getReceiver().getNickname()) // 받는 사람 닉네임
                        .requesterProfileImageUrl(req.getReceiver().getProfileImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    // 친구 신청 취소 (철회)
    @Transactional
    public void cancelFriendRequest(Integer userId, Integer requestId) {
        // 1. 요청 조회
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // 2. 권한 검증
        if (!request.getRequester().getId().equals(userId)) {
            throw new CustomException(GlobalErrorCode.NOT_REQUESTER);
        }

        // 3. 상태 검증 (이미 수락/거절된 건 취소 불가 -> PENDING만 가능)
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new CustomException(GlobalErrorCode.CANNOT_CANCEL_PROCESSED_REQUEST);
        }

        // 4. 연관관계 제거 및 삭제
        request.getRequester().getSentFriendRequests().removeIf(r -> r.getId().equals(requestId));
        request.getReceiver().getReceivedFriendRequests().removeIf(r -> r.getId().equals(requestId));

        friendRequestRepository.delete(request);
    }
}
