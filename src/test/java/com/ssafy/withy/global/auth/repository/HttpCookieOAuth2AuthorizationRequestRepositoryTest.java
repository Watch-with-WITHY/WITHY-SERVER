package com.ssafy.withy.global.auth.repository;

import com.ssafy.withy.global.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationRequestRepositoryTest {

    @Test
    @DisplayName("OAuth2 요청 객체를 쿠키 값으로 직렬화/역직렬화 할 수 있다.")
    void serializationTest() {
        // given
        OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://google.com/auth")
                .clientId("test-client-id")
                .redirectUri("http://localhost:8080/callback")
                .scope("email", "profile")
                .state("test-state")
                .build();

        // when
        String cookieValue = CookieUtils.serialize(request);
        Cookie cookie = new Cookie("test", cookieValue);
        OAuth2AuthorizationRequest result = CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAuthorizationUri()).isEqualTo(request.getAuthorizationUri());
        assertThat(result.getClientId()).isEqualTo(request.getClientId());
        assertThat(result.getState()).isEqualTo(request.getState());
        System.out.println("✅ 직렬화 테스트 통과: " + cookieValue);
    }

    @Test
    @DisplayName("쿠키를 굽고(Save) 다시 꺼낼(Load) 수 있다.")
    void cookieSaveAndLoadTest() {
        // given
        HttpCookieOAuth2AuthorizationRequestRepository repository = new HttpCookieOAuth2AuthorizationRequestRepository();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthorizationRequest authRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://google.com/auth")
                .clientId("client-123")
                .redirectUri("http://callback")
                .state("random-state")
                .build();

        // when: 저장
        repository.saveAuthorizationRequest(authRequest, request, response);

        // then: 저장된 쿠키 확인
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotEmpty();

        // when: 로드 (요청에 쿠키를 담아서 다시 보냄)
        request.setCookies(cookies);
        OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(request);

        // then: 검증
        assertThat(loadedRequest).isNotNull();
        assertThat(loadedRequest.getState()).isEqualTo("random-state");
        System.out.println("✅ 쿠키 저장/로드 테스트 통과");
    }
}