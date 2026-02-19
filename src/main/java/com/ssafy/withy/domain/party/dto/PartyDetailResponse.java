package com.ssafy.withy.domain.party.dto;

import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.entity.PlatformType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartyDetailResponse(
        Integer partyId,
        String title,
        String hostName,
        String hostProfileImage,

        // 컨텐츠 정보
        Integer contentId, // 우리 DB의 ID
        Integer tmdbId, // TMDB ID
        String contentTitle,
        String posterPath,
        String backdropPath,
        PlatformType platform,
        String externalId,

        // 상태 정보
        int currentParticipants,
        int maxParticipants,
        boolean isDeleted,
        boolean isActive,
        boolean isPrivate,
        LocalDateTime scheduledActiveTime,
        LocalDateTime createdAt,
        String startUrl) {
    public static PartyDetailResponse from(Party party) {
        String url = null;
        if (party.getContent() != null && party.getContent().getExternalId() != null) {
            if (party.getPlatform() == PlatformType.YOUTUBE) {
                url = "https://www.youtube.com/watch?v=" + party.getContent().getExternalId();
            } else if (party.getPlatform() == PlatformType.OTT) {
                url = "https://www.netflix.com/watch/" + party.getContent().getExternalId();
            }
        }

        return PartyDetailResponse.builder()
                .partyId(party.getId())
                .title(party.getTitle())
                .hostName(party.getHost().getNickname())
                .hostProfileImage(party.getHost().getProfileImageUrl())
                // Content 정보 매핑
                .contentId(party.getContent().getId())
                .tmdbId(party.getContent().getTmdbId())
                .contentTitle(party.getContent().getTitle())
                .externalId(party.getContent().getExternalId())
                .posterPath(party.getContent().getPosterPath())
                .platform(party.getPlatform())
                // Party 정보 매핑
                .currentParticipants(party.getCurrentParticipants())
                .maxParticipants(party.getMaxParticipants())
                .isDeleted(party.getIsDeleted())
                .isActive(party.getIsActive())
                .isPrivate(party.getIsPrivate())
                .scheduledActiveTime(party.getScheduledActiveTime())
                .createdAt(party.getCreatedAt())
                .startUrl(url)
                .build();
    }
}