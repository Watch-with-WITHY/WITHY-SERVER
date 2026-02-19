package com.ssafy.withy.global.api.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// TMDB
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbResponse(
        List<TmdbResultDto> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbResultDto(
            Long id,

            // [핵심] 영화는 title, 드라마는 name으로 옴 -> 하나로 합침
            @JsonAlias({"title", "name"})
            String title,

            // [핵심] 영화는 release_date, 드라마는 first_air_date
            @JsonAlias({"release_date", "first_air_date"})
            String releaseDate,

            String overview,

            @JsonProperty("original_language")
            String originalLanguage,

            @JsonProperty("poster_path")
            String posterPath,

            @JsonProperty("media_type")
            String mediaType // "movie", "tv", "person" 등이 옴
    ) {
        public record TmdbGenre(Integer id, String name) {}
    }
}