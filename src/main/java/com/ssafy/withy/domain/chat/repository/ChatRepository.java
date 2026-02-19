package com.ssafy.withy.domain.chat.repository;

import com.ssafy.withy.domain.chat.entity.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<ChatLog, Integer> {

    @Query("SELECT c FROM ChatLog c JOIN FETCH c.user WHERE c.id = :id")
    Optional<ChatLog> findByIdWithUser(@Param("id") Integer id);
}
