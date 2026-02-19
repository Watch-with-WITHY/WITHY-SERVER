package com.ssafy.withy.global.auth.handler;

import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.auth.jwt.JwtTokenProvider;
import com.ssafy.withy.global.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.global.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.ssafy.withy.global.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @org.springframework.beans.factory.annotation.Value("${spring.frontend.base-url}")
    private String frontendBaseUrl;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 1. 어디로 갈지 정한다! (쿠키 우선, 없으면 기본값)
        String targetUrl = determineTargetUrl(request, response, authentication);

        // 2. 구글 이메일 조회
        String email = (String) attributes.get("email");

        // 3. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 4. 토큰 생성 및 DB 저장
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), email, user.getRole().getKey());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), email);

        user.updateRefreshToken(refreshToken);
        userRepository.saveAndFlush(user);

        // 4. 리다이렉트 URL 생성
        String redirectUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        // 6. 사용한 쿠키 청소
        clearAuthenticationAttributes(request, response);

        // 7. 프론트로
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // 어디로 갈지 결정하는 메서드
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        // 쿠키에 있으면 거기로 가고, 없으면 기본값(배포 주소)으로 가라
        return redirectUri.orElse(frontendBaseUrl + "/oauth/callback");
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}