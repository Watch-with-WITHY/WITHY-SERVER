package com.ssafy.withy.domain.party.dto;

import com.ssafy.withy.domain.content.entity.MediaType;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.entity.PlatformType;

import java.time.LocalDateTime;
import java.util.List;

public record PartyListResponseDto(
    Integer id,
    String title,
    PlatformType platform,
    MediaType mediaType,
    List<String> genreNames,
    Integer currentParticipants,
    Integer maxParticipants,
    Boolean isActive,
    Boolean isPrivate,            // 비밀번호 설정 여부
    LocalDateTime scheduledActiveTime, // 활성화 예정 시간
    Integer currentPlaybackTime,  // 현재 재생 진행 시간 (초 단위, nullable)
    String thumbnail,             // 컨텐츠 썸네일 경로
    HostInfo host                  // 호스트 정보
) {
    /**
     * 호스트 정보를 담는 중첩 record
     */
    public record HostInfo(
        Integer userId,
        String nickname,
        String profileImageUrl
    ) {}
    
    /**
     * Party 엔티티를 PartyListResponseDto로 변환
     * @param party 파티 엔티티
     * @param genreNames 장르 이름 리스트
     * @param currentPlaybackTime 현재 재생 진행 시간 (초 단위, 시청 기록 없으면 null)
     * @param host 호스트 정보
     * @return PartyListResponseDto
     */
    public static PartyListResponseDto from(
        Party party, 
        List<String> genreNames,
        Integer currentPlaybackTime,
        HostInfo host
    ) {
        return new PartyListResponseDto(
            party.getId(),
            party.getTitle(),
            party.getPlatform(),
            party.getContent() != null ? party.getContent().getMediaType() : null,
            genreNames,
            party.getCurrentParticipants(),
            party.getMaxParticipants(),
            party.getIsActive(),
            party.getIsPrivate(),
            party.getScheduledActiveTime(),
            currentPlaybackTime,
            party.getContent() != null ? party.getContent().getPosterPath() : null,
            host
        );
    }
}
