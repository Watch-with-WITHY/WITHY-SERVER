package com.ssafy.withy.domain.dm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DmRoomRequest {
    
    @NotNull(message = "상대방 사용자 ID는 필수입니다.")
    private Integer targetUserId;
}
