package com.ssafy.withy.global.auth.dto;

import com.ssafy.withy.domain.user.entity.LoginType;
import com.ssafy.withy.domain.user.entity.Role;
import com.ssafy.withy.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;


    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String picture) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        // 구글 로그인인지 확인
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public User toEntity(String profileImageUrl, String nickname) {
        return User.builder()
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl) // 전달받은 이미지 URL 사용
                .loginType(LoginType.GOOGLE)
                .role(Role.USER) // 가입 시 기본 권한은 USER
                .isActive(true)  // 활성화 상태 기본값
                .preferredLanguage("ko") // 기본 언어 설정
                .build();
    }
}