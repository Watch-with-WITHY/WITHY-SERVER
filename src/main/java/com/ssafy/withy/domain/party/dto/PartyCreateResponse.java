package com.ssafy.withy.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 생성 응답")
public record PartyCreateResponse(
        @Schema(description = "생성된 파티 ID", example = "1") Integer partyId) {
}
