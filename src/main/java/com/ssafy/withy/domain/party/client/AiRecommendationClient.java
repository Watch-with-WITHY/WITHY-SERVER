package com.ssafy.withy.domain.party.client;

import com.ssafy.withy.domain.party.dto.AiRecommendationRequest;
import com.ssafy.withy.domain.party.dto.AiRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * AI 추천 서버와 통신하는 클라이언트
 * Fail-Open 정책: AI 서버 장애 시 빈 리스트 반환하여 Fallback 로직 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiRecommendationClient {

    private final RestTemplate restTemplate;

    @Value("${ai.recommendation.url}")
    private String aiRecommendationUrl;

    /**
     * AI 서버에 추천 요청을 보내고 추천 영화 ID 목록을 반환
     * 
     * @param onboardingGenres 사용자 선호 장르
     * @param activePartyMovies 현재 활성화된 파티의 영화 ID 목록
     * @param topK 추천 받을 개수
     * @return 추천 영화 ID 목록 (실패 시 빈 리스트)
     */
    public List<Integer> getRecommendations(
            Integer userId,
            List<String> onboardingGenres,
            List<Integer> activePartyMovies,
            Integer topK) {

        try {
            // 요청 DTO 생성
            AiRecommendationRequest request = AiRecommendationRequest.builder()
                    .userId(userId)
                    .onboardingGenres(onboardingGenres)
                    .activePartyMovies(activePartyMovies)
                    .topK(topK)
                    .build();

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiRecommendationRequest> entity = new HttpEntity<>(request, headers);

            // AI 서버 호출
            log.info("AI 추천 서버 호출: URL={}, topK={}, genres={}, activeMovies={}",
                    aiRecommendationUrl, topK, onboardingGenres.size(), activePartyMovies.size());

            AiRecommendationResponse response = restTemplate.postForObject(
                    aiRecommendationUrl + "/api/v1/recommend",
                    entity,
                    AiRecommendationResponse.class
            );

            if (response == null || response.getRecommendations() == null) {
                log.warn("AI 서버 응답이 null입니다. Fallback 사용");
                return List.of();
            }

            List<Integer> movieIds = response.getRecommendations().stream()
                    .map(rec -> rec.getMovieId())
                    .toList();

            log.info("AI 추천 성공: {} 개의 영화 추천받음", movieIds.size());
            return movieIds;

        } catch (Exception e) {
            // Fail-Open: 예외 발생 시 빈 리스트 반환하여 Fallback 로직 사용
            log.warn("AI 추천 서버 호출 실패, Fallback 사용: {}", e.getMessage());
            return List.of();
        }
    }
}
