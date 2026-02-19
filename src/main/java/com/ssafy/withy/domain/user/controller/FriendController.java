package com.ssafy.withy.domain.user.controller;

import com.ssafy.withy.domain.user.dto.FriendDto;
import com.ssafy.withy.domain.user.dto.FriendRequestCreateRequest;
import com.ssafy.withy.domain.user.dto.FriendRequestHandleRequest;
import com.ssafy.withy.domain.user.dto.FriendRequestResponse;
import com.ssafy.withy.domain.user.service.FriendService;
import com.ssafy.withy.global.auth.dto.CustomUserDetails;
import com.ssafy.withy.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Friend", description = "친구 관리 API")
@RestController
@RequestMapping("/api/v1/users/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "친구 신청 발송", description = "다른 사용자에게 친구 신청을 보냅니다.")
    @PostMapping("/requests")
    public ApiResponse<Void> sendFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody FriendRequestCreateRequest request) {

        friendService.sendFriendRequest(userDetails.getUserId(), request.receiverId());

        return ApiResponse.success(200, "친구 신청을 보냈습니다.", null);
    }

    @Operation(summary = "친구 신청 수락/거절", description = "받은 친구 신청을 수락하거나 거절합니다.")
    @PatchMapping("/requests/{requestId}")
    public ApiResponse<Void> handleFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer requestId,
            @RequestBody FriendRequestHandleRequest request) {

        friendService.handleFriendRequest(userDetails.getUserId(), requestId, request.isAccepted());

        return ApiResponse.success(200, "친구 신청을 처리했습니다.", null);
    }

    @Operation(summary = "받은 친구 신청 목록 조회", description = "나에게 온 친구 신청 중 대기 상태인 목록을 조회합니다.")
    @GetMapping("/requests/received")
    public ApiResponse<List<FriendRequestResponse>> getReceivedFriendRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<FriendRequestResponse> response = friendService.getReceivedFriendRequests(userDetails.getUserId());

        return ApiResponse.success(200, "받은 친구 신청 목록 조회 성공", response);
    }

    @Operation(summary = "친구 목록 조회", description = "나의 친구 목록을 조회합니다.")
    @GetMapping("")
    public ApiResponse<List<FriendDto>> getFriendList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<FriendDto> response = friendService.getFriendList(userDetails.getUserId());

        return ApiResponse.success(200, "친구 목록 조회 성공", response);
    }

    @Operation(summary = "보낸 친구 신청 목록 조회", description = "내가 보낸 친구 신청 중 대기(PENDING) 상태인 목록을 조회합니다.")
    @GetMapping("/requests/sent")
    public ApiResponse<List<FriendRequestResponse>> getSentFriendRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(200, "친구 신청 목록 조회에 성공했습니다.",
                friendService.getSentFriendRequests(userDetails.getUserId()));
    }

    @Operation(summary = "친구 신청 취소", description = "보냈던 친구 신청을 철회(삭제)합니다.")
    @DeleteMapping("/requests/{requestId}")
    public ApiResponse<Void> cancelFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer requestId) {
        friendService.cancelFriendRequest(userDetails.getUserId(), requestId);
        return ApiResponse.success(200, "친구 신청이 정상적으로 취소되었습니다.", null);
    }
}
