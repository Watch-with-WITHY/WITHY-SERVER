package com.ssafy.withy.domain.party.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartySyncRequest {
    private Integer partyId;
    private Long currentPosition; // 초 단위 or 밀리초 (프론트와 협의, 여기서는 단순 중계)
}
