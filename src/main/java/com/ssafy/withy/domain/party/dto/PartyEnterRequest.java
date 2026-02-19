package com.ssafy.withy.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PartyEnterRequest(
        @Schema(description = "비공개 파티 비밀번호 (공개 파티일 경우 null)", example = "1234")
        String password
) {}