package com.ssafy.withy.domain.party.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 추천 서버로부터 받는 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationResponse {

    private List<AiMovieRecommendation> recommendations;
}
