package com.ssafy.withy.domain.party.dto;

import com.ssafy.withy.domain.content.dto.GenreDto;
import java.util.List;

public record IntegratedSearchResponseDto(
        List<GenreDto> categories,
        List<PartyListResponseDto> parties) {
    public static IntegratedSearchResponseDto of(List<GenreDto> categories, List<PartyListResponseDto> parties) {
        return new IntegratedSearchResponseDto(categories, parties);
    }
}
