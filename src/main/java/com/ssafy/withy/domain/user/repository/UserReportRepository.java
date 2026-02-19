package com.ssafy.withy.domain.user.repository;

import com.ssafy.withy.domain.chat.entity.ChatLog;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserReportRepository extends JpaRepository<UserReport, Integer> {
    boolean existsByReporterAndChatLog(User reporter, ChatLog chatLog);

    /**
     * 사용자의 모든 신고 내역 삭제 (회원 탈퇴용)
     */
    @Modifying
    @Query("DELETE FROM UserReport ur WHERE ur.reporter = :user OR ur.reported = :user")
    void deleteAllByReporterOrReported(@Param("user") User user);
}
