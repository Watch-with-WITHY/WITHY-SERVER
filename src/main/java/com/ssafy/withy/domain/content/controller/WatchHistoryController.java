package com.ssafy.withy.domain.content.controller;

import com.ssafy.withy.domain.content.dto.WatchHistoryResponse;
import com.ssafy.withy.domain.content.dto.WatchHistorySaveRequest;
import com.ssafy.withy.domain.content.dto.WatchHistorySaveResponse;
import com.ssafy.withy.domain.content.service.WatchHistoryService;
import com.ssafy.withy.global.auth.dto.CustomUserDetails;
import com.ssafy.withy.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "WatchHistory", description = "시청 기록 관리 API")
@RestController
@RequestMapping("/api/v1/users/me/histories")
@RequiredArgsConstructor
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @Operation(summary = "시청 기록 저장", description = "컨텐츠 시청 기록을 저장하거나 갱신합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ApiResponse<WatchHistorySaveResponse> saveWatchHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WatchHistorySaveRequest request) {
        return ApiResponse.success(200, "시청 기록이 저장되었습니다.",
                watchHistoryService.saveOrUpdateHistory(userDetails.getUserId(), request));
    }

    @Operation(summary = "시청 기록 조회", description = "사용자의 시청 기록 목록을 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ApiResponse<List<WatchHistoryResponse>> getMyWatchHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(200, "시청 기록 조회에 성공했습니다.",
                watchHistoryService.getMyWatchHistories(userDetails.getUserId(), pageable));
    }
}
