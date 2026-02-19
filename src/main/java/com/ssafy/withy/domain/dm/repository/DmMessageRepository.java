package com.ssafy.withy.domain.dm.repository;

import com.ssafy.withy.domain.dm.entity.DmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DmMessageRepository extends JpaRepository<DmMessage, Integer> {
    
    // 특정 DM 방의 메시지 목록 조회 (페이지네이션, 최신순)
    // @EntityGraph로 N+1 문제 방지 (sender 정보 함께 조회)
    @EntityGraph(attributePaths = {"sender"})
    Page<DmMessage> findByRoom_IdOrderByCreatedAtDesc(Integer roomId, Pageable pageable);

    // 특정 DM 방의 메시지 목록 조회 (JoinedAt 이후 메시지만, 페이지네이션, 최신순)
    @EntityGraph(attributePaths = {"sender"})
    Page<DmMessage> findByRoom_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Integer roomId, java.time.LocalDateTime joinedAt, Pageable pageable);
}
