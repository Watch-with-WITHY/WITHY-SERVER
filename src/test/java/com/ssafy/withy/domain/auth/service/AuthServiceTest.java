package com.ssafy.withy.domain.auth.service;

import com.ssafy.withy.domain.user.dto.SignUpRequest;
import com.ssafy.withy.domain.user.entity.Role;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.auth.dto.TokenResponse;
import com.ssafy.withy.global.auth.jwt.JwtTokenProvider;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.global.util.RandomNicknameGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*; // anyLong()을 위해 *로 변경하거나 구체적으로 import
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Mock
    private RandomNicknameGenerator randomNicknameGenerator;

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Test
    @DisplayName("일반 로그인 성공")
    void login_success() {
        // given
        String email = "test@test.com";
        String password = "password123!";
        User user = User.builder()
                .id(1) // ID가 있어야 토큰에 넣을 수 있음
                .email(email)
                .password("encodedPassword")
                .nickname("nickname")
                .role(Role.USER)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, user.getPassword())).willReturn(true);

        // [수정됨 ⭐] createAccessToken(userId, email, role) 순서!
        // user.getId()가 1(Integer)이지만 Service에서 Long으로 넘긴다고 가정하고 1L로 매칭
        // 만약 Entity ID가 Integer라면 Service에서 int -> long 자동 변환됨.
        given(jwtTokenProvider.createAccessToken(1, email, "ROLE_USER")).willReturn("accessToken");

        // [수정됨 ⭐] createRefreshToken(userId, email) 순서!
        given(jwtTokenProvider.createRefreshToken(1, email)).willReturn("refreshToken");

        // when
        TokenResponse response = authService.login(email, password);

        // then
        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("refreshToken", response.refreshToken());
        assertEquals(1, response.userId());
        assertEquals("nickname", response.nickname());

        then(userRepository).should(times(1)).findByEmail(email);
        then(passwordEncoder).should(times(1)).matches(password, user.getPassword());

        // [수정됨 ⭐] 검증도 파라미터 맞춰줘야 함
        then(jwtTokenProvider).should(times(1)).createAccessToken(1, email, "ROLE_USER");
        then(jwtTokenProvider).should(times(1)).createRefreshToken(1, email);
    }

    @Test
    @DisplayName("일반 로그인 실패 - 사용자 없음")
    void login_fail_user_not_found() {
        // given
        String email = "test@test.com";
        String password = "password123!";

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> authService.login(email, password));
        assertEquals(GlobalErrorCode.LOGIN_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("일반 로그인 실패 - 비밀번호 불일치")
    void login_fail_wrong_password() {
        // given
        String email = "test@test.com";
        String password = "password123!";
        User user = User.builder()
                .email(email)
                .password("encodedPassword")
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, user.getPassword())).willReturn(false);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> authService.login(email, password));
        assertEquals(GlobalErrorCode.LOGIN_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() {
        // given
        String refreshToken = "validRefreshToken";
        String email = "test@test.com";
        User user = User.builder()
                .id(1)
                .email(email)
                .refreshToken(refreshToken)
                .role(Role.USER)
                .nickname("nickname")
                .build();

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getEmail(refreshToken)).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // [수정됨 ⭐] userId(1L) 추가
        given(jwtTokenProvider.createAccessToken(1, email, "ROLE_USER")).willReturn("newAccessToken");
        given(jwtTokenProvider.createRefreshToken(1, email)).willReturn("newRefreshToken");

        // when
        TokenResponse response = authService.reissue(refreshToken);

        // then
        assertNotNull(response);
        assertEquals("newAccessToken", response.accessToken());
        assertEquals("newRefreshToken", response.refreshToken());
        assertEquals(1, response.userId());
        assertEquals("nickname", response.nickname());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 형식")
    void reissue_fail_invalid_token_format() {
        // given
        String refreshToken = "invalidFormat";
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(false);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> authService.reissue(refreshToken));
        assertEquals(GlobalErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 저장된 토큰과 불일치 (탈취 의심)")
    void reissue_fail_token_mismatch() {
        // given
        String refreshToken = "validRefreshToken";
        String email = "test@test.com";
        User user = User.builder()
                .email(email)
                .refreshToken("differentToken") // DB에는 다른 토큰이 있음
                .build();

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getEmail(refreshToken)).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> authService.reissue(refreshToken));
        assertEquals(GlobalErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        // given
        String refreshToken = "validRefreshToken";
        String email = "test@test.com";
        User user = User.builder()
                .email(email)
                .refreshToken(refreshToken)
                .build();

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getEmail(refreshToken)).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        authService.logout(refreshToken);

        // then
        assertNull(user.getRefreshToken());
    }

    @Test
    @DisplayName("회원가입 성공 - 자동 로그인")
    void signup_success() {
        // given
        SignUpRequest request = new SignUpRequest("test@test.com", "password123!");

        // 1. 기존 Mocking
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        given(valueOperations.get("Verified:" + request.email())).willReturn("true");

        User savedUser = User.builder()
                .id(1)
                .email(request.email())
                .password("encodedPassword")
                .nickname(null)
                .role(Role.USER)
                .build();

        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtTokenProvider.createAccessToken(anyInt(), anyString(), anyString())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(anyInt(), anyString())).willReturn("refreshToken");

        // when
        TokenResponse response = authService.signup(request);

        // then
        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("refreshToken", response.refreshToken());

        then(userRepository).should(times(1)).existsByEmail(request.email());
        then(userRepository).should(times(1)).save(any(User.class));

        then(redisTemplate).should(times(1)).delete("Verified:" + request.email());

        then(jwtTokenProvider).should(times(1)).createAccessToken(anyInt(), anyString(), anyString());
        then(jwtTokenProvider).should(times(1)).createRefreshToken(anyInt(), anyString());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_fail_duplicate_email() {
        // given
        SignUpRequest request = new SignUpRequest("test@test.com", "password123!");

        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> authService.signup(request));

        assertEquals(GlobalErrorCode.USER_DUPLICATE_EMAIL, exception.getErrorCode());

        then(userRepository).should(never()).save(any(User.class));
    }

    @Nested
    @DisplayName("로그인 시 온보딩 상태 반환 테스트")
    class LoginOnboardingTest {

        @Test
        @DisplayName("신규 유저(온보딩 미완료)는 false를 반환해야 한다.")
        void login_NewUser_ReturnsFalse() {
            // given
            String email = "newbie@test.com";
            String password = "password123";

            // 1. 온보딩 안 한(false) 유저 준비
            User user = User.builder()
                    .email(email)
                    .password("encodedPassword")
                    .isOnboardingComplete(false)
                    .role(Role.USER)
                    .build();

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(password, user.getPassword())).willReturn(true);

            // 토큰 발급 Mocking (테스트 흐름 방해 안 되게 아무거나 리턴)
            given(jwtTokenProvider.createAccessToken(any(), any(), any())).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(any(), any())).willReturn("refresh-token");

            // when
            TokenResponse response = authService.login(email, password);

            // then
            // 2. 응답 DTO에 false가 잘 들어갔는지 확인
            assertThat(response.isOnboardingComplete()).isFalse();
        }

        @Test
        @DisplayName("기존 유저(온보딩 완료)는 true를 반환해야 한다.")
        void login_OldUser_ReturnsTrue() {
            // given
            String email = "oldbie@test.com";
            String password = "password123";

            // 1. 온보딩 완료한(true) 유저 준비
            User user = User.builder()
                    .email(email)
                    .password("encodedPassword")
                    .isOnboardingComplete(true)
                    .role(Role.USER)
                    .build();

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(password, user.getPassword())).willReturn(true);

            given(jwtTokenProvider.createAccessToken(any(), any(), any())).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(any(), any())).willReturn("refresh-token");

            // when
            TokenResponse response = authService.login(email, password);

            // then
            // 2. 응답 DTO에 true가 잘 들어갔는지 확인
            assertThat(response.isOnboardingComplete()).isTrue();
        }
    }
}