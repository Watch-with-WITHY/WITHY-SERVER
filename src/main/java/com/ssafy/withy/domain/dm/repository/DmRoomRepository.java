package com.ssafy.withy.domain.dm.repository;

import com.ssafy.withy.domain.dm.entity.DmRoom;
import com.ssafy.withy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DmRoomRepository extends JpaRepository<DmRoom, Integer> {
    
    // 두 사용자 간 DM 방 찾기 (순서 무관)
    @Query("SELECT dr FROM DmRoom dr WHERE " +
           "(dr.userA.id = :userId1 AND dr.userB.id = :userId2) OR " +
           "(dr.userA.id = :userId2 AND dr.userB.id = :userId1)")
    Optional<DmRoom> findByTwoUsers(@Param("userId1") Integer userId1, 
                                     @Param("userId2") Integer userId2);
    
    // 특정 사용자가 참여 중인 모든 DM 방 조회 (최근 메시지 순, 나간 방 제외)
    @Query("SELECT dr FROM DmRoom dr WHERE " +
           "((dr.userA.id = :userId AND dr.userALeftAt IS NULL) OR " +
           "(dr.userB.id = :userId AND dr.userBLeftAt IS NULL)) " +
           "ORDER BY dr.lastMessageAt DESC NULLS LAST, dr.createdAt DESC")
    List<DmRoom> findAllByUserId(@Param("userId") Integer userId);

    List<DmRoom> findAllByUserAOrUserB(User userA, User userB);
}
