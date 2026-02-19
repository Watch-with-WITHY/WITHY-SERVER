package com.ssafy.withy.global.auth.service;

import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.auth.dto.OAuthAttributes;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.global.util.RandomNicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final RandomNicknameGenerator nicknameGenerator;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = getDelegate();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 서비스 구분 (google)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        // PK가 되는 필드 이름 (구글은 "sub")
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        // DTO로 변환
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName,
                oAuth2User.getAttributes());

        // 저장 또는 업데이트
        User user = saveOrUpdate(attributes);

        // SecurityContext에 저장할 UserDetails 객체 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())), // Role Enum에 getRoleKey() 메서드 필요
                                                                                      // (ex: "ROLE_USER")
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .orElse(null);

        if (user == null) {
            String nickname = generateUniqueNickname();
            user = attributes.toEntity(User.DEFAULT_PROFILE_IMAGE_URL, nickname);
        }

        // 닉네임이 없는 경우 (기존 유저 포함) 강제 생성 -> 버그 수정
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            String nickname = generateUniqueNickname();
            user.updateNickname(nickname);
        }

        return userRepository.save(user);
    }

    private String generateUniqueNickname() {
        String nickname;
        int maxRetry = 10;
        do {
            nickname = nicknameGenerator.generate();
            maxRetry--;
            if (maxRetry < 0) throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }

    protected OAuth2UserService<OAuth2UserRequest, OAuth2User> getDelegate() {
        return new DefaultOAuth2UserService();
    }
}