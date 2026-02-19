package com.ssafy.withy.domain.content.dto;

import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.party.entity.PlatformType;

public record GenreDto(
        Integer id,
        String name,
        Integer code,
        PlatformType type) {
    public static GenreDto from(Genre genre) {
        return new GenreDto(
                genre.getId(),
                genre.getName(),
                genre.getCode(),
                genre.getType());
    }
}
