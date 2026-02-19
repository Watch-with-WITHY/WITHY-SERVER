package com.ssafy.withy.domain.content.repository;

import com.ssafy.withy.domain.content.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.withy.domain.content.entity.MediaType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Integer> {
        Optional<WatchHistory> findFirstByUserIdAndContent_MediaTypeOrderByCreatedAtDesc(Integer userId,
                        MediaType mediaType);

        Optional<WatchHistory> findFirstByUserIdOrderByCreatedAtDesc(Integer userId);

        @Query("SELECT wh FROM WatchHistory wh WHERE wh.user.id = :userId AND wh.content.id IN :contentIds " +
                        "AND wh.createdAt = (SELECT MAX(wh2.createdAt) FROM WatchHistory wh2 " +
                        "WHERE wh2.user.id = :userId AND wh2.content.id = wh.content.id)")
        List<WatchHistory> findLatestByUserIdAndContentIds(@Param("userId") Integer userId,
                        @Param("contentIds") List<Integer> contentIds);

        // 사용자+컨텐츠 조합으로 시청 기록 조회 (업데이트 여부 판단용)
        Optional<WatchHistory> findByUserIdAndContentId(Integer userId, Integer contentId);

        // 사용자의 시청 기록 목록 조회 (최신순)
        List<WatchHistory> findByUserIdOrderByUpdatedAtDesc(Integer userId, Pageable pageable);

        @Modifying
        @Query("DELETE FROM WatchHistory wh WHERE wh.user.id = :userId")
        void deleteAllByUserId(@Param("userId") Integer userId);

        // 특정 미디어 타입 제외하고 시청 기록 조회 (예: YOUTUBE 제외)
        List<WatchHistory> findByUserIdAndContent_MediaTypeNotOrderByUpdatedAtDesc(Integer userId, MediaType mediaType, Pageable pageable);
}
