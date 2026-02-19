package com.ssafy.withy.domain.party.entity;

import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.ssafy.withy.domain.content.entity.Content;
import com.ssafy.withy.domain.chat.entity.ChatLog;

@Entity
@Table(name = "parties")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Party extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private PlatformType platform;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "actual_active_time")
    private LocalDateTime actualActiveTime;

    @Column(name = "scheduled_active_time", nullable = false)
    private LocalDateTime scheduledActiveTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate;

    @Column(name = "password", length = 64)
    private String password;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "current_participants", nullable = false)
    private Integer currentParticipants;

    @Column(name = "episode_number")
    private Integer episodeNumber;

    @Column(name = "season_number")
    private Byte seasonNumber;

    @Builder.Default
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "party", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ChatLog> chatLogs = new ArrayList<>();

    @OneToOne(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private PartyState partyState;

    public void deleteParty() {
        this.isDeleted = true;
        this.isActive = false; // 삭제됐으니 당연히 비활성
    }

    // 파티 활성화
    public void activate() {
        this.isActive = true;
        this.actualActiveTime = LocalDateTime.now();
    }

    // 파티 정보 수정
    public void update(String title, int maxParticipants, boolean isPrivate, String password, Content content,
            PlatformType platform) {
        this.title = title;
        this.maxParticipants = maxParticipants;
        this.isPrivate = isPrivate;
        // 공개로 전환되면 비밀번호 삭제, 비공개 유지/전환이면 새 비밀번호 or 기존 유지
        if (!isPrivate) {
            this.password = null;
        } else if (password != null && !password.isBlank()) {
            this.password = password;
        }

        this.content = content;
        this.platform = platform;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void changeHost(User newHost) {
        this.host = newHost;
    }

    public void increaseCurrentParticipants() {
        this.currentParticipants++;
    }

    public void decreaseCurrentParticipants() {
        if (this.currentParticipants > 0) {
            this.currentParticipants--;
        }
    }
}
