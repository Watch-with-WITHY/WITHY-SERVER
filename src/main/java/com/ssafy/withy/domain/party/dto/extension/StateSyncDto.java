package com.ssafy.withy.domain.party.dto.extension;

import com.ssafy.withy.domain.party.entity.CommandType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateSyncDto {
    private CommandType commandType;
    private Double currentPosition;
    private Boolean isPlaying;
    private String partyUrl;
    private Long timestamp;
}
