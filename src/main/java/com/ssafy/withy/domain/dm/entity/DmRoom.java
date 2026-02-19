package com.ssafy.withy.domain.dm.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.ssafy.withy.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dm_rooms")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class DmRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DmMessage> messages = new ArrayList<>();

    @Column(name = "user_a_left_at")
    private LocalDateTime userALeftAt;

    @Column(name = "user_b_left_at")
    private LocalDateTime userBLeftAt;

    @Column(name = "user_a_joined_at")
    private LocalDateTime userAJoinedAt;

    @Column(name = "user_b_joined_at")
    private LocalDateTime userBJoinedAt;

    public void updateLastMessage(String message, LocalDateTime time) {
        this.lastMessage = message;
        this.lastMessageAt = time;
    }

    public void leave(Integer userId) {
        if (this.userA.getId().equals(userId)) {
            this.userALeftAt = LocalDateTime.now();
        } else if (this.userB.getId().equals(userId)) {
            this.userBLeftAt = LocalDateTime.now();
        }
    }

    public void rejoin(Integer userId, LocalDateTime rejoinTime) {
        if (this.userA.getId().equals(userId)) {
            this.userALeftAt = null;
            this.userAJoinedAt = rejoinTime;
        } else if (this.userB.getId().equals(userId)) {
            this.userBLeftAt = null;
            this.userBJoinedAt = rejoinTime;
        }
    }

    public boolean isLeft(Integer userId) {
        if (this.userA.getId().equals(userId)) {
            return this.userALeftAt != null;
        } else if (this.userB.getId().equals(userId)) {
            return this.userBLeftAt != null;
        }
        return false;
    }

    public LocalDateTime getJoinedAt(Integer userId) {
        if (this.userA.getId().equals(userId)) {
            return this.userAJoinedAt;
        } else if (this.userB.getId().equals(userId)) {
            return this.userBJoinedAt;
        }
        return null; // Should ideally not happen if properly initialized
    }
    
    // Lifecycle hook to set initial joinedAt
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.userAJoinedAt == null) {
            this.userAJoinedAt = this.createdAt;
        }
        if (this.userBJoinedAt == null) {
            this.userBJoinedAt = this.createdAt;
        }
    }
}
