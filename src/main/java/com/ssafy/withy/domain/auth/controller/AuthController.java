package com.ssafy.withy.domain.auth.controller;

import com.ssafy.withy.domain.auth.dto.EmailRequest;
import com.ssafy.withy.domain.auth.dto.EmailVerifyRequest;
import com.ssafy.withy.domain.auth.dto.TokenRequest;
import com.ssafy.withy.domain.auth.service.AuthService;
import com.ssafy.withy.domain.user.dto.SignUpRequest;
import com.ssafy.withy.global.auth.dto.LoginRequest;
import com.ssafy.withy.global.auth.dto.TokenResponse;
import com.ssafy.withy.global.common.code.GlobalSuccessCode;
import com.ssafy.withy.global.common.response.ApiResponse;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.global.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증(로그인/토큰) API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final EmailService emailService;

    @Operation(summary = "일반 회원가입", description = "회원가입 후 자동으로 로그인되어 토큰을 발급합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(@Valid @RequestBody SignUpRequest request) {
        TokenResponse response = authService.signup(request);

        return ResponseEntity
                .status(GlobalSuccessCode.USER_SIGNUP_SUCCESS.getStatus())
                .body(ApiResponse.success(GlobalSuccessCode.USER_SIGNUP_SUCCESS, response));
    }

    @Operation(summary = "일반 로그인", description = "이메일/비번으로 로그인하고 토큰을 받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.LOGIN_SUCCESS, response));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용해 Access Token을 갱신합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody @Valid TokenRequest request) { // [수정] String -> TokenRequest
        TokenResponse response = authService.reissue(request.refreshToken());

        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.REISSUE_SUCCESS, response));
    }

    @Operation(summary = "로그아웃", description = "서버에서 Refresh Token을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody @Valid TokenRequest request) { // [수정] String -> TokenRequest
        authService.logout(request.refreshToken());

        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.LOGOUT_SUCCESS, null));
    }

    @Operation(summary = "이메일 인증번호 전송", description = "회원가입을 위한 인증번호를 이메일로 전송합니다.")
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<GlobalSuccessCode>> sendEmailCode(@RequestBody @Valid EmailRequest request) {

        authService.sendJoinCode(request.email());
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.EMAIL_SEND_SUCCESS));
    }

    @Operation(summary = "이메일 인증번호 검증", description = "전송된 인증번호를 확인합니다.")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<GlobalSuccessCode>> verifyEmailCode(@RequestBody @Valid EmailVerifyRequest request) {
        boolean verified = emailService.verifyCode(request.email(), request.code());
        if (!verified) {
            throw new CustomException(GlobalErrorCode.INVALID_AUTH_CODE);
        }
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.EMAIL_VERIFY_SUCCESS));
    }
}