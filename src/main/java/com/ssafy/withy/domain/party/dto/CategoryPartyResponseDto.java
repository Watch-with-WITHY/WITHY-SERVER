package com.ssafy.withy.domain.party.dto;

import com.ssafy.withy.domain.content.dto.GenreDto;
import java.util.List;

public record CategoryPartyResponseDto(
        GenreDto genre,
        List<PartyListResponseDto> parties) {
    public static CategoryPartyResponseDto of(GenreDto genre, List<PartyListResponseDto> parties) {
        return new CategoryPartyResponseDto(genre, parties);
    }
}
