package com.ssafy.withy.domain.content.controller;

import com.ssafy.withy.domain.content.dto.GenreListResponseDto;
import com.ssafy.withy.domain.content.service.GenreService;
import com.ssafy.withy.global.common.code.GlobalSuccessCode;
import com.ssafy.withy.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Genre", description = "장르(카테고리) 관련 API")
@Validated
@RestController
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @Operation(summary = "장르 목록 조회", description = "플랫폼 타입에 따른 전체 장르 목록을 조회합니다.",
            parameters = {
                    @Parameter(name = "platform", description = "플랫폼 타입 (OTT, YOUTUBE)", example = "OTT")
            })
    @GetMapping
    public ResponseEntity<ApiResponse<GenreListResponseDto>> getGenreList(
            @RequestParam(name = "platform") @NotBlank(message = "플랫폼 타입은 필수값입니다.") String platform
    ) {
        GenreListResponseDto result = genreService.getGenreList(platform);
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.GET_GENRE_LIST_SUCCESS, result));
    }
}
