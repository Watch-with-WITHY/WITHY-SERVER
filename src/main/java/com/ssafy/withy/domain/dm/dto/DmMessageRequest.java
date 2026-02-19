package com.ssafy.withy.domain.dm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DmMessageRequest {

    @NotNull(message = "DM 방 ID는 필수입니다.")
    private Integer roomId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String message;
}
