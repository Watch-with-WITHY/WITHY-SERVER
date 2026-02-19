package com.ssafy.withy.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "플랫폼 타입 리스트 응답 DTO")
public record PlatformTypeResponseDto(
        @Schema(description = "플랫폼 타입 리스트", example = "[\"OTT\", \"YOUTUBE\"]") List<String> types) {
    public static PlatformTypeResponseDto from(List<String> types) {
        return PlatformTypeResponseDto.builder()
                .types(types)
                .build();
    }
}
