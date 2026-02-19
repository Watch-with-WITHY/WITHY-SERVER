package com.ssafy.withy.domain.auth.service;

import com.ssafy.withy.domain.user.dto.SignUpRequest;
import com.ssafy.withy.domain.user.entity.LoginType;
import com.ssafy.withy.domain.user.entity.Role;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.entity.UserStatus;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.auth.dto.TokenResponse;
import com.ssafy.withy.global.auth.jwt.JwtTokenProvider;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.global.service.EmailService;
import com.ssafy.withy.global.util.RandomNicknameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RandomNicknameGenerator nicknameGenerator;
    @Value("${spring.cloud.aws.s3.base-url}")
    private String s3BaseUrl;
    private static final String DEFAULT_PROFILE_PATH = "/profile/default.png";

    private final RedisTemplate<String, String> redisTemplate;
    /**
     * 일반 로그인
     */
    @Transactional
    public TokenResponse login(String email, String password) {
        // 1. 이메일 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LOGIN_FAILED));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(GlobalErrorCode.LOGIN_FAILED);
        }

        // 3. 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleKey());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        // 4. 리프레시 토큰 DB 저장
        user.updateRefreshToken(refreshToken);
        user.updateStatus(UserStatus.ONLINE);

        return new TokenResponse(accessToken, refreshToken, user.getId(), user.getNickname(), user.isOnboardingComplete(), user.getLoginType());
    }

    /**
     * 토큰 재발급 (Reissue)
     */
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        // 1. 리프레시 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. 토큰에서 유저 이메일 추출
        String email = jwtTokenProvider.getEmail(refreshToken);

        // 3. DB에서 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 4. DB에 저장된 토큰과 일치하는지 확인
        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 5. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleKey());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        // 6. DB 업데이트
        user.updateRefreshToken(newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken, user.getId(), user.getNickname(), user.isOnboardingComplete(), user.getLoginType());
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        user.updateRefreshToken(null);
        user.updateStatus(UserStatus.OFFLINE);
    }

    /**
     * 회원가입
     */
    @Transactional
    public TokenResponse signup(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(GlobalErrorCode.USER_DUPLICATE_EMAIL);
        }

        String nickname = generateUniqueNickname();

        // 성능 테스트 계정은 이메일 인증 스킵
        if (!request.email().startsWith("host_poll_") && !request.email().startsWith("guest_poll_") &&
                !request.email().startsWith("host_sync_") && !request.email().startsWith("guest_sync_")) {
            String isVerified = redisTemplate.opsForValue().get("Verified:" + request.email());
            if (isVerified == null || !isVerified.equals("true")) {
                throw new CustomException(GlobalErrorCode.EMAIL_NOT_VERIFIED);
            }

            // 3. 인증 기록 삭제 (재가입 방지 or 일회성 사용)
            redisTemplate.delete("Verified:" + request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        String fullProfileUrl = s3BaseUrl + DEFAULT_PROFILE_PATH;

        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(nickname)
                .loginType(LoginType.LOCAL)
                .role(Role.USER)
                .profileImageUrl(fullProfileUrl)
                .preferredLanguage("ko")
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRoleKey());
        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getId(), savedUser.getEmail());

        user.updateRefreshToken(refreshToken);

        return new TokenResponse(accessToken, refreshToken, user.getId(), user.getNickname(), user.isOnboardingComplete(), user.getLoginType());
    }

    public void sendJoinCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(GlobalErrorCode.ALREADY_EXIST_EMAIL);
        }

        emailService.sendVerificationCode(email);
    }

    private String generateUniqueNickname() {
        String nickname;
        int maxRetry = 10; // 무한루프 방지용 안전장치

        do {
            nickname = nicknameGenerator.generate();
            maxRetry--;
            if (maxRetry < 0) {
                throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
            }
        } while (userRepository.existsByNickname(nickname)); // 중복이면 다시!

        return nickname;
    }
}