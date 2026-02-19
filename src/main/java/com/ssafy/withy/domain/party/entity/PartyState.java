package com.ssafy.withy.domain.party.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "party_states")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class PartyState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private Party party;

    @Column(name = "party_url", nullable = false, length = 256)
    private String partyUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_command")
    private CommandType lastCommand;

    @Column(name = "current_position", nullable = false)
    private Integer currentPosition;

    @Column(name = "is_playing", nullable = false)
    private Boolean isPlaying;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(String partyUrl, CommandType lastCommand, Integer currentPosition, Boolean isPlaying) {
        this.partyUrl = partyUrl;
        this.lastCommand = lastCommand;
        this.currentPosition = currentPosition;
        this.isPlaying = isPlaying;
    }
}
