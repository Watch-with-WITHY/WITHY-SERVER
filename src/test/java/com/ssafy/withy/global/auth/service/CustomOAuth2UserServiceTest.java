package com.ssafy.withy.global.auth.service;

import com.ssafy.withy.domain.user.entity.Role;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.util.RandomNicknameGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RandomNicknameGenerator nicknameGenerator;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> mockDelegate;

    @Test
    @DisplayName("소셜 로그인 - 구글 사용자 로드 성공")
    void loadUser_google_success() {
        // given
        CustomOAuth2UserService customOAuth2UserService = new CustomOAuth2UserService(userRepository, nicknameGenerator) {
            @Override
            protected OAuth2UserService<OAuth2UserRequest, OAuth2User> getDelegate() {
                return mockDelegate;
            }
        };

        // 2. Prepare Mock Data
        String registrationId = "google";
        String userNameAttributeName = "sub";
        Map<String, Object> attributes = Map.of(
                "sub", "123456789",
                "name", "Test User",
                "email", "test@gmail.com",
                "picture", "http://profile.url");
        OAuth2User oAuth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                userNameAttributeName);

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(registrationId)
                .userNameAttributeName(userNameAttributeName)
                .clientId("clientId")
                .clientSecret("clientSecret")
                .authorizationGrantType(
                        org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("redirectUri")
                .authorizationUri("authUri")
                .tokenUri("tokenUri")
                .userInfoUri("userInfoUri")
                .build();

        OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration,
                mock(org.springframework.security.oauth2.core.OAuth2AccessToken.class));

        given(mockDelegate.loadUser(userRequest)).willReturn(oAuth2User);

        User savedUser = User.builder()
                .email("test@gmail.com")
                .nickname("Test User")
                .role(Role.USER)
                .build();

        // Mocking saveOrUpdate logic:
        // Case 1: New User
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        // then
        assertNotNull(result);
        assertEquals("Test User", result.getAttributes().get("name"));

        then(userRepository).should(times(1)).findByEmail("test@gmail.com");
        then(userRepository).should(times(1)).save(any(User.class));
    }
}
