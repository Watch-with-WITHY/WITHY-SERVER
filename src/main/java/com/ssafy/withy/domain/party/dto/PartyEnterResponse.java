package com.ssafy.withy.domain.party.dto;

import com.ssafy.withy.domain.content.entity.MediaType;
import com.ssafy.withy.domain.party.entity.ParticipantRole;
import com.ssafy.withy.domain.party.entity.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "파티 입장 성공 응답 DTO")
public class PartyEnterResponse {

    @Schema(description = "파티 ID", example = "1")
    private Integer partyId;

    @Schema(description = "참여자 역할 (HOST, GUEST)", example = "GUEST")
    private ParticipantRole role;

    @Schema(description = "호스트 여부 (이 사람이 방장인가?)", example = "false")
    private Boolean isHost;

    @Schema(description = "파티 활성화 여부 (지금 같이 보고 있는 중인가?)", example = "true")
    private Boolean isActive;

    @Schema(description = "플랫폼 (OTT, YOUTUBE)", example = "OTT")
    private PlatformType platform;

    @Schema(description = "미디어 타입 (MOVIE, TV, YOUTUBE)", example = "MOVIE")
    private MediaType mediaType;

    @Schema(description = "컨텐츠 ID (내부 ID)", example = "100")
    private Integer contentId;

    @Schema(description = "TMDB ID (영화/시리즈 식별용)", example = "550")
    private Integer tmdbId;

    @Schema(description = "외부 플랫폼 ID (유튜브 VideoID 등)", example = "dQw4w9WgXcQ")
    private String externalId;

    @Schema(description = "컨텐츠 제목", example = "오징어 게임 시즌2")
    private String title;

    @Schema(description = "썸네일/포스터 경로", example = "http://example.com/image.jpg")
    private String thumbnail;

    @Schema(description = "시즌 번호 (시리즈인 경우)", example = "1")
    private Byte seasonNumber;

    @Schema(description = "에피소드 번호 (시리즈인 경우)", example = "1")
    private Integer episodeNumber;

    @Schema(description = "현재 재생 시점 (초 단위, 시청 기록이 있으면 그 시간, 없으면 0)", example = "120")
    private Integer currentPlaybackTime;

    public static PartyEnterResponse of(
            Integer partyId,
            ParticipantRole role,
            Boolean isActive,
            PlatformType platform,
            MediaType mediaType,
            Integer contentId,
            Integer tmdbId,
            String externalId,
            String title,
            String thumbnail,
            Byte seasonNumber,
            Integer episodeNumber,
            Integer currentPlaybackTime
    ) {
        return PartyEnterResponse.builder()
                .partyId(partyId)
                .role(role)
                .isHost(role == ParticipantRole.HOST)
                .isActive(isActive)
                .platform(platform)
                .mediaType(mediaType)
                .contentId(contentId)
                .tmdbId(tmdbId)
                .externalId(externalId)
                .title(title)
                .thumbnail(thumbnail)
                .seasonNumber(seasonNumber)
                .episodeNumber(episodeNumber)
                .currentPlaybackTime(currentPlaybackTime)
                .build();
    }
}
