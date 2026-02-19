package com.ssafy.withy.domain.content.controller;

import com.ssafy.withy.domain.content.dto.WatchHistoryResponse;
import com.ssafy.withy.domain.content.service.WatchHistoryService;
import com.ssafy.withy.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI Integration", description = "AI 서버 연동 API (API Key Required)")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiWatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @Operation(summary = "특정 사용자 시청 기록 조회 (AI 전용)", description = "AI 서버가 추천 학습을 위해 특정 사용자의 시청 기록을 조회합니다.")
    @GetMapping("/users/{userId}/histories")
    public ApiResponse<List<com.ssafy.withy.domain.content.dto.AiWatchHistoryResponse>> getUserWatchHistories(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // 여기에 도달했다는 것은 이미 ApiKeyAuthenticationFilter를 통해 ROLE_AI_SYSTEM 권한을 획득했다는 의미
        
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(200, "AI - 시청 기록 조회 성공",
                watchHistoryService.getMyWatchHistoriesForAi(userId, pageable));
    }
}
