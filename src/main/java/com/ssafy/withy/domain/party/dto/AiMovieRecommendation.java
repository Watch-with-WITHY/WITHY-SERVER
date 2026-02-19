package com.ssafy.withy.domain.party.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 추천 서버로부터 받는 개별 영화 추천 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiMovieRecommendation {

    @JsonProperty("movie_id")
    private Integer movieId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("genres")
    private List<String> genres;
}
