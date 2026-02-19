package com.ssafy.withy.domain.user.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GenreSubscribeRequest(
        @NotEmpty(message = "최소 1개 이상의 장르를 선택해야 합니다.")
        List<Integer> genreIds // 장르 ID 리스트 (ex: [12, 18, 35])
) {}
