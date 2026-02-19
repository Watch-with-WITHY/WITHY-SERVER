package com.ssafy.withy.domain.chat.service;

import com.ssafy.withy.domain.chat.dto.ChatLogDto;
import com.ssafy.withy.domain.chat.dto.SpoilerCheckRequest;
import com.ssafy.withy.domain.chat.dto.SpoilerCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpoilerClient {

    @Value("${ai.spoiler.url:https://dino-granulocytic-lyda.ngrok-free.dev}")
    private String aiSpoilerUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * AI 서버(Service B)에 스포일러 검사를 요청한다.
     * REST API (POST /api/v1/spoiler/check)
     */
    public SpoilerCheckResponse checkSpoiler(String moviePlot, String partyId, List<ChatLogDto> messages) {
        try {
            log.info("Sending request to AI Spoiler Check. URL: {}, PartyID: {}, Msg Count: {}", aiSpoilerUrl, partyId, messages.size());
            
            SpoilerCheckRequest request = SpoilerCheckRequest.builder()
                    .moviePlot(moviePlot)
                    .partyId(partyId)
                    .messages(messages)
                    .build();

            SpoilerCheckResponse response = restClient.post()
                    .uri(aiSpoilerUrl + "/api/v1/spoiler/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SpoilerCheckResponse.class);
            
            if (response != null) {
                log.info("Received response from AI. PartyID: {}, Result Count: {}", partyId, response.getResults() != null ? response.getResults().size() : "null");
            } else {
                log.warn("Received NULL response from AI for PartyID: {}", partyId);
            }
            
            return response;

        } catch (Exception e) {
            log.error("Failed to check spoiler for party {}: {}", partyId, e.getMessage(), e); // Stack trace 포함
            // 장애 시 정책: 빈 결과 반환 -> 스포일러 없음으로 처리 (Fail-open)
            return null;
        }
    }
}
