package com.ssafy.withy.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 추천 서버로 전송하는 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 추천 요청")
public class AiRecommendationRequest {

    @Schema(description = "사용자 ID (추천 개인화용)", example = "10")
    private Integer userId;

    @Schema(description = "사용자가 선택한 선호 장르 목록", example = "[\"Action\", \"Sci-Fi\"]")
    private List<String> onboardingGenres;

    @Schema(description = "현재 활성화된 파티들의 영화 ID 목록", example = "[550, 999]")
    private List<Integer> activePartyMovies;

    @Schema(description = "추천 받을 항목 개수 (기본값: 4, 최대: 50)", example = "4")
    private Integer topK;
}
