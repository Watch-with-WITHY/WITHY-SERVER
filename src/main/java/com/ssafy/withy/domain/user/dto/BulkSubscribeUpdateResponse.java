package com.ssafy.withy.domain.user.dto;

import com.ssafy.withy.domain.content.dto.GenreDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "장르 구독 일괄 업데이트 응답")
public record BulkSubscribeUpdateResponse(
        @Schema(description = "현재 구독 중인 장르 목록") List<GenreDto> subscribedGenres) {
    public static BulkSubscribeUpdateResponse from(List<GenreDto> genreDtos) {
        return new BulkSubscribeUpdateResponse(genreDtos);
    }
}
