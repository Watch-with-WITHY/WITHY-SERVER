package com.ssafy.withy.domain.content.dto;

import java.util.List;

public record GenreListResponseDto(
        List<GenreDto> genres
) {
    public static GenreListResponseDto from(List<GenreDto> genres) {
        return new GenreListResponseDto(genres);
    }
}
