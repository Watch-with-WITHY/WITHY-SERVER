package com.ssafy.withy.domain.user.controller;

import com.ssafy.withy.domain.user.dto.*;
import com.ssafy.withy.domain.content.dto.GenreListResponseDto;
import com.ssafy.withy.domain.content.dto.MyChatLogResponse;
import com.ssafy.withy.domain.user.service.FriendService;
import com.ssafy.withy.domain.user.service.UserReportService;
import com.ssafy.withy.domain.user.service.UserService;
import com.ssafy.withy.global.auth.dto.CustomUserDetails;
import com.ssafy.withy.global.common.code.GlobalSuccessCode;
import com.ssafy.withy.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

@Tag(name = "User", description = "회원 관리 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FriendService friendService;
    private final UserReportService userReportService;

    @Operation(summary = "차단 목록 조회", description = "본인이 차단한 유저 목록 전체를 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/blocks")
    public ApiResponse<List<BlockListResponse>> getBlockList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Service에도 userId만 넘기도록 수정
        return ApiResponse.success(200, "차단 목록 조회에 성공했습니다.",
                userService.getBlockList(userDetails.getUserId()));
    }

    @Operation(summary = "타 유저 프로필 조회", description = "특정 유저의 상세 프로필을 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUserProfile(@PathVariable Integer userId) {
        return ApiResponse.success(200, "유저 프로필 조회에 성공했습니다.", userService.getUserProfile(userId));
    }

    @Operation(summary = "유저 차단", description = "특정 유저를 차단합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/blocks")
    public ApiResponse<Void> blockUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody BlockUserRequest request) {
        userService.blockUser(userDetails.getUserId(), request.blockedId());
        return ApiResponse.success(200, "사용자를 차단했습니다.", null);
    }

    @Operation(summary = "유저 차단 해제", description = "차단해둔 유저의 차단을 해제합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/blocks/{blockedId}")
    public ApiResponse<Void> unblockUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer blockedId) {
        userService.unblockUser(userDetails.getUserId(), blockedId);
        return ApiResponse.success(200, "차단을 해제했습니다.", null);
    }

    @Operation(summary = "친구 삭제", description = "친구 관계를 끊습니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/friends/{friendId}")
    public ApiResponse<Void> deleteFriend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer friendId) {
        friendService.deleteFriend(userDetails.getUserId(), friendId);
        return ApiResponse.success(200, "친구를 삭제했습니다.", null);
    }

    @Operation(summary = "유저 신고", description = "특정 채팅 메시지를 기반으로 유저를 신고합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/reports")
    public ApiResponse<Void> reportUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserReportRequest request) {
        userReportService.reportUser(userDetails.getUserId(), request);
        return ApiResponse.success(200, "사용자를 신고했습니다.", null);
    }

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 상세 프로필 정보를 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(GlobalSuccessCode.GET_MY_PROFILE_SUCCESS,
                userService.getMyProfile(userDetails.getUserId()));
    }


    @Operation(summary = "내 정보 수정", description = "닉네임과 프로필 이미지를 수정합니다.")
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart(value = "request", required = false) UpdateUserRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        UserProfileResponse response = userService.updateMyProfile(userDetails.getUserId(), request, profileImage);

        return ResponseEntity.ok(ApiResponse.success(
                GlobalSuccessCode.USER_UPDATE_SUCCESS,
                response
        ));
    }

    @Operation(summary = "선호 언어 변경", description = "유저의 선호 언어(preferredLanguage)를 변경합니다.")
    @PatchMapping("/language")
    public ApiResponse<Void> updatePreferredLanguage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserLanguageUpdateRequest request
    ) {
        userService.updatePreferredLanguage(userDetails.getUserId(), request.language());
        return ApiResponse.success(GlobalSuccessCode.USER_LANGUAGE_UPDATE_SUCCESS, null);
    }

    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자를 탈퇴 처리(비활성화) 합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me")
    public ApiResponse<Void> withdrawUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.withdrawUser(userDetails.getUserId());
        return ApiResponse.success(GlobalSuccessCode.USER_WITHDRAWAL_SUCCESS, null);
    }

    @Operation(summary = "내 구독 리스트 조회", description = "로그인한 사용자가 구독 중인 장르(카테고리) 목록을 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/subscribes")
    public ApiResponse<GenreListResponseDto> getSubscriptionList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(GlobalSuccessCode.GET_MY_SUBSCRIBE_LIST_SUCCESS,
                userService.getSubscriptions(userDetails.getUserId()));
    }

    @Operation(summary = "장르 구독 해제", description = "내 구독 리스트에서 특정 장르를 삭제(구독 해제)합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/subscribes/{genreId}")
    public ApiResponse<Void> unsubscribeGenre(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer genreId) {
        userService.unsubscribeGenre(userDetails.getUserId(), genreId);
        return ApiResponse.success(GlobalSuccessCode.UNSUBSCRIBE_SUCCESS, null);
    }

    @Operation(summary = "장르 구독 일괄 업데이트", description = "구독할 장르 목록을 한 번에 업데이트합니다. 기존 구독은 모두 삭제되고 요청한 장르만 구독됩니다. 빈 배열 전송 시 모든 구독이 해제됩니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/subscribes")
    public ApiResponse<BulkSubscribeUpdateResponse> updateSubscriptions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BulkSubscribeUpdateRequest request) {
        return ApiResponse.success(
                GlobalSuccessCode.BULK_UPDATE_SUBSCRIPTION_SUCCESS,
                userService.updateSubscriptions(userDetails.getUserId(), request.genreIds()));
    }

    @Operation(summary = "내 채팅 로그 조회", description = "사용자가 작성한 채팅 로그 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/chat-logs")
    public ApiResponse<List<MyChatLogResponse>> getMyChatLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(GlobalSuccessCode.GET_MY_CHAT_LOGS_SUCCESS,
                userService.getMyChatLogs(userDetails.getUserId(), pageable));
    }

    @Operation(summary = "이메일 중복 체크", description = "true: 중복(사용불가), false: 사용가능")
    @GetMapping("/email/check")
    public ResponseEntity<ApiResponse<CheckDuplicateResponse>> checkEmail(@RequestParam String email) {
        CheckDuplicateResponse response = userService.checkEmailDuplicate(email);
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.CHECK_EMAIL_SUCCESS, response));
    }

    @Operation(summary = "닉네임 중복 체크", description = "true: 중복(사용불가), false: 사용가능")
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<CheckDuplicateResponse>> checkNickname(@RequestParam String nickname) {
        CheckDuplicateResponse response = userService.checkNicknameDuplicate(nickname);
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.CHECK_NICKNAME_SUCCESS, response));
    }

    @Operation(summary = "랜덤 닉네임 생성", description = "형용사+명사 조합의 랜덤 닉네임을 반환합니다.")
    @GetMapping("/nickname/random")
    public ResponseEntity<ApiResponse<RandomNicknameResponse>> getRandomNickname() {
        RandomNicknameResponse response = userService.generateRandomNickname();
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.GENERATE_NICKNAME_SUCCESS, response));
    }

    @Operation(summary = "유저 닉네임 설정(변경)", description = "최초 가입 시 또는 닉네임 변경 시 사용합니다.")
    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid NicknameUpdateRequest request) {

        userService.updateNickname(user.getUserId(), request.nickname());
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.UPDATE_NICKNAME_SUCCESS, null));
    }

    @Operation(summary = "장르 구독(선호 설정)", description = "유저가 선호하는 장르를 설정합니다. (기존 설정 덮어쓰기)")
    @PostMapping("/me/preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid GenreSubscribeRequest request) {

        userService.updateGenrePreferences(user.getUserId(), request.genreIds());
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.UPDATE_PREFERENCES_SUCCESS, null));
    }

    @Operation(summary = "닉네임으로 유저 검색", description = "닉네임이 정확히 일치하는 유저를 단건 조회합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<UserSearchResponse>> searchUser(
            @Parameter(description = "검색할 닉네임 (정확히 일치해야 함)", required = true)
            @RequestParam String nickname,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UserSearchResponse response = userService.searchUserByNickname(nickname, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인하고 새로운 비밀번호로 변경합니다.")

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        userService.changePassword(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.USER_PASSWORD_CHANGE_SUCCESS));
    }


    @Operation(summary = "온보딩 완료 처리", description = "신규 가입 유저가 온보딩 과정을 마쳤음을 표시합니다.")
    @PatchMapping("/onboarding")
    public ResponseEntity<ApiResponse<Void>> completeOnboarding(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.completeOnboarding(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.USER_ONBOARDING_COMPLETE));
    }
}
