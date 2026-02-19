package com.ssafy.withy.global.auth.jwt;

import com.ssafy.withy.domain.user.entity.Role;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.global.auth.dto.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey key;

    // 24시간
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24;
    // 7일
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. 토큰 생성
    public String createAccessToken(Integer userId, String email, String role) {
        long expireTime = ACCESS_TOKEN_EXPIRE_TIME;

        if (isTestAccount(email)) {
            expireTime = 1000L * 60 * 60 * 24 * 365 * 100; // 100년 (사실상 무제한)
        }

        return createToken(userId, email, role, expireTime);
    }

    public String createRefreshToken(Integer userId, String email) {
        long expireTime = REFRESH_TOKEN_EXPIRE_TIME;

        if (isTestAccount(email)) {
            expireTime = 1000L * 60 * 60 * 24 * 365 * 100;
        }
        return createToken(userId, email, null, expireTime);
    }

    private String createToken(Integer userId, String email, String role, long expireTime) {
        Date now = new Date();

        JwtBuilder builder = Jwts.builder()
                .subject(email) // sub: 이메일
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireTime))
                .signWith(key);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    // 2. 인증 객체 조회
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        // 정보 추출
        Integer userId = claims.get("userId", Integer.class); // 저장했던 ID 꺼내기
        String email = claims.getSubject();
        String roleString = claims.get("role", String.class);

        if (userId == null || roleString == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // Role 변환
        String enumName = roleString.replace("ROLE_", "");
        Role role = Role.valueOf(enumName);

        // [핵심] DB 안 가고 메모리에서 User 객체 생성
        User principalUser = User.builder()
                .id(userId)
                .email(email)
                .role(role)
                .isActive(true) // 토큰이 유효하면 활성 유저로 간주
                .build();

        // CustomUserDetails 생성
        CustomUserDetails userDetails = new CustomUserDetails(principalUser);

        // 권한 목록 생성
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(new String[] { roleString })
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    // 3. 토큰 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTestAccount(String email) {
        if (email == null) return false;

        return List.of(
                "test@withy.com",
                "demo@withy.com",
                "willy_1020@naver.com",
                "jungyo0416@naver.com",
                "ho2762@naver.com",
                "yjsong2153@gmail.com",
                "gyqls080813@naver.com",
                "duddnr216@naver.com",
                "subaccforpaying@gmail.com"
        ).contains(email);
    }
}