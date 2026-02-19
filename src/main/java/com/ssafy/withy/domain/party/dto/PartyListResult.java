package com.ssafy.withy.domain.party.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PartyListResult(
        List<PartyListResponseDto> parties,
        int totalPage,
        long totalElements
) {
    public static PartyListResult of(Page<PartyListResponseDto> page) {
        return new PartyListResult(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }
}
