package com.ssafy.withy.domain.user.repository;

import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Integer> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    void deleteByBlockerAndBlocked(User blocker, User blocked);

    @Modifying
    @Query("DELETE FROM UserBlock ub WHERE ub.blocker = :user OR ub.blocked = :user")
    void deleteAllByBlockerOrBlocked(@Param("user") User user);

    List<UserBlock> findAllByBlockerIdOrderByCreatedAtDesc(Integer blockerId);
}
