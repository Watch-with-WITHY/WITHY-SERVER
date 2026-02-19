package com.ssafy.withy.global.config;

import com.ssafy.withy.global.auth.filter.ApiKeyAuthenticationFilter;
import com.ssafy.withy.global.auth.handler.OAuth2LoginSuccessHandler;
import com.ssafy.withy.global.auth.jwt.JwtAuthenticationFilter;
import com.ssafy.withy.global.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.ssafy.withy.global.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    // 허용할 URL 목록
    private static final String[] PERMIT_ALL_PATTERNS = {
            "/api/v1/auth/**", // 로그인 관련
            "/oauth2/**", // 소셜 로그인 관련
            "/login/**",
            "/swagger-ui/**", // 스웨거
            "/v3/api-docs/**",
            "/error",
            "/api/v1/users/signup",
            "/api/v1/users/*/check",
            "/chat-test.html", // 채팅 테스트 페이지
            "/test_client.html", // 익스텐션 테스트 페이지
            "/ws/**", // 웹소켓 연결 (Legacy)
            "/ws-stomp/**", // 웹소켓 연결 (New)
            "/api/test/**" // 성능 테스트용 (임시)
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 세션 사용 안 함 (STATELESS)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. URL 접근 권한 설정
                .authorizeHttpRequests(request -> request
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PERMIT_ALL_PATTERNS).permitAll()
                        .anyRequest().authenticated())

                // 인증/인가 실패 시 처리
                .exceptionHandling(exception -> exception
                        // 인증 실패
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"UNAUTHORIZED\", \"message\": \"인증이 필요합니다.\"}");
                        })
                        // 권한 없음
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"FORBIDDEN\", \"message\": \"접근 권한이 없습니다.\"}");
                        }))

                // 5. 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository))
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(401);
                            response.getWriter().write("로그인 실패 이유: " + exception.getMessage());
                        }))

                // 6. 커스텀 필터 등록 (UsernamePasswordAuthenticationFilter 이전에 실행)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 설정 (프론트엔드와 통신을 위해 필수)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 주소 허용 (localhost:3000, 배포 도메인 등)
        configuration.setAllowedOriginPatterns(
                List.of("http://localhost:3000","https://watchwithwithy.vercel.app",
                        "http://localhost:8080", "http://3.36.95.236.nip.io",
                        "https://3.36.95.236.nip.io", "chrome-extension://*",
                        "https://www.netflix.com", "https://www.youtube.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // 쿠키나 인증 헤더 허용

        configuration.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> cookieProcessorCustomizer() {
        return (factory) -> factory.addContextCustomizers((context) -> {
            Rfc6265CookieProcessor processor = new Rfc6265CookieProcessor();

            processor.setSameSiteCookies(SameSiteCookies.NONE.getValue());

            context.setCookieProcessor(processor);
        });
    }
}
