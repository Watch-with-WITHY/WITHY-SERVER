package com.ssafy.withy.domain.chat.repository;

import com.ssafy.withy.domain.chat.entity.ChatLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.ssafy.withy.domain.user.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatLogRepository extends JpaRepository<ChatLog, Integer> {
    // 사용자의 채팅 로그 조회 (최신순)
    List<ChatLog> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ChatLog cl WHERE cl.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
