package com.ssafy.withy.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 비밀번호 응답")
public record PartyPasswordResponse(
        @Schema(description = "파티 비밀번호", example = "1234")
        String password
) {
}
