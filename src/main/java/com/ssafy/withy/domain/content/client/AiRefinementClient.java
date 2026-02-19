package com.ssafy.withy.domain.content.client;

import com.ssafy.withy.domain.content.entity.Content;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRefinementClient {

    private final RestTemplate restTemplate;

    @Value("${ai.refinement.url}")
    private String aiRefinementUrl;

    @Async
    public void requestRefinement(Content content) {
        try {
            String url = aiRefinementUrl + "/api/v1/contents/refine";

            RefinementRequest request = RefinementRequest.builder()
                    .content_id(content.getId())
                    .tmdb_id(content.getTmdbId())
                    .title(content.getTitle())
                    .eng_title(content.getOriginalTitle())
                    .media_type(content.getMediaType().name())
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<RefinementRequest> entity = new HttpEntity<>(request, headers);

            log.info("AI Refinement 요청: contentId={}, title={}", content.getId(), content.getTitle());

            // Fire-and-forget
            restTemplate.postForObject(url, entity, Map.class);
            
        } catch (Exception e) {
            // Fail-Open: 정제 요청 실패해도 서비스는 계속 진행
            log.warn("AI Refinement 요청 실패 (Fail-Open): {}", e.getMessage());
        }
    }

    @Builder
    private record RefinementRequest(
            Integer content_id,
            Integer tmdb_id,
            String title,
            String eng_title,
            String media_type
    ) {}
}
