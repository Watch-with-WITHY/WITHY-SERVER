package com.ssafy.withy.global.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.auth.api-key}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        
        // AI 전용 경로가 아니면 필터 통과 (인증 없이 downstream으로 가거나, 다른 필터가 처리)
        // 단, 헤더가 있는데 경로가 틀리면...? -> 보안상 무시하거나 로깅. 여기선 무시하고 진행.
        if (!requestUri.startsWith("/api/v1/ai/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-KEY");

        if (apiKey != null && validApiKey.equals(apiKey)) {
            log.info("API Key authentication success. Remote Addr: {}", request.getRemoteAddr());
            // Create an authentication object for AI System
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "AI-System", 
                    null, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_AI_SYSTEM"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
             // 경로가 /api/v1/ai/ 인데 키가 없거나 틀리면 401
             response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
             response.setContentType("application/json");
             response.setCharacterEncoding("UTF-8");
             response.getWriter().write("{\"status\": 401, \"message\": \"Invalid or missing API Key\"}");
             return;
        }

        filterChain.doFilter(request, response);
    }
}
