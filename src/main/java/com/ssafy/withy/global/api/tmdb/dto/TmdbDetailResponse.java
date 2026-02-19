package com.ssafy.withy.global.api.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbDetailResponse(
        Integer id,
        String title, // Movie
        String name,  // TV
        String overview,

        @JsonProperty("original_language")
        String originalLanguage, // [핵심] ko, en, ja ...

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("release_date")
        String releaseDate, // Movie

        @JsonProperty("first_air_date")
        String firstAirDate, // TV

        List<Genre> genres // [핵심] Search API랑 다르게 객체로 옴!
) {
    public record Genre(Integer id, String name) {}
}