package com.ssafy.withy.domain.user.repository;

import com.ssafy.withy.domain.user.entity.FriendRequest;
import com.ssafy.withy.domain.user.entity.FriendRequestStatus;
import com.ssafy.withy.domain.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Integer> {
    boolean existsByRequesterAndReceiverAndStatus(User requester, User receiver, FriendRequestStatus status);

    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequestStatus status);

    // 1. 내가 보낸 요청 목록 (상태가 PENDING인 것만, 받는 사람 정보 같이 로딩)
    @Query("SELECT fr FROM FriendRequest fr JOIN FETCH fr.receiver WHERE fr.requester.id = :senderId AND fr.status = 'PENDING'")
    List<FriendRequest> findAllSentRequests(@Param("senderId") Integer senderId);

    // 2. 특정 유저에게 보낸 대기 중인 요청이 있는지 확인 (DTO용)
    boolean existsByRequester_IdAndReceiver_IdAndStatus(Integer requesterId, Integer receiverId, FriendRequestStatus status);
}
