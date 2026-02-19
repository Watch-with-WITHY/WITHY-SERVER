package com.ssafy.withy.domain.party.entity;

import jakarta.validation.constraints.NotNull;

public record ParticipantRoleUpdateRequest(
        @NotNull(message = "변경할 권한은 필수입니다.")
        ParticipantRole newRole
) {}
