package com.ssafy.withy.domain.party.client;

import com.ssafy.withy.domain.party.dto.AiContextCachingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AI 컨텍스트 캐싱 클라이언트
 * - 파티 생성/수정 시 영화 줄거리를 AI 서버(vLLM)에 미리 캐싱 요청
 * - Fail-Open 정책: 오류 발생 시 서비스 로직에 영향 주지 않고 로그만 기록
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiContextCachingClient {

    private final RestTemplate restTemplate;

    @Value("${ai.recommendation.url}")
    private String aiContextUrl;

    @Async
    public void cacheContext(String partyId, String moviePlot) {
        if (moviePlot == null || moviePlot.isBlank()) {
            return;
        }

        try {
            String url = aiContextUrl + "/api/v1/context/cache";
            
            AiContextCachingRequest request = AiContextCachingRequest.builder()
                    .partyId(partyId)
                    .moviePlot(moviePlot)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiContextCachingRequest> entity = new HttpEntity<>(request, headers);

            log.info("AI Context Caching 요청: partyId={}, plotLength={}", partyId, moviePlot.length());
            
            // Fire-and-forget에 가깝지만, 응답을 확인하여 로그 남김
            Map response = restTemplate.postForObject(url, entity, Map.class);
            log.info("AI Context Caching 성공: response={}", response);

        } catch (Exception e) {
            // Fail-Open: 캐싱 실패해도 파티 생성/수정은 계속 진행
            log.warn("AI Context Caching 실패 (Fail-Open): {}", e.getMessage());
        }
    }
}
