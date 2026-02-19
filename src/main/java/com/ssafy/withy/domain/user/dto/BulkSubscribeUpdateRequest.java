package com.ssafy.withy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "장르 구독 일괄 업데이트 요청")
public record BulkSubscribeUpdateRequest(
        @Schema(description = "구독할 장르 ID 리스트 (빈 배열 시 전체 해제)", example = "[1, 3, 5, 7]") @NotNull(message = "genreIds는 필수입니다.") @Size(max = 50, message = "최대 50개까지 구독 가능합니다.") List<Integer> genreIds) {
    public BulkSubscribeUpdateRequest {
        if (genreIds != null) {
            genreIds = genreIds.stream()
                    .distinct() // 중복 제거
                    .toList();
        }
    }
}
