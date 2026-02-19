package com.ssafy.withy.domain.chat.service;

import com.ssafy.withy.domain.chat.dto.AiServerRequest;
import com.ssafy.withy.domain.chat.dto.AiServerResponse;
import com.ssafy.withy.domain.chat.dto.TranslationRequest;
import com.ssafy.withy.domain.chat.dto.TranslationResponse;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private final WebClient webClient;
    
    // AI 서버 주소가 바뀔 수 있으므로 프로퍼티로 관리하면 좋지만, 우선 하드코딩 후 나중에 리팩토링도 고려
    @Value("${ai.server.url:https://dino-granulocytic-lyda.ngrok-free.dev}")
    private String aiServerUrl;

    public TranslationResponse translate(TranslationRequest request, Long userId) {
        
        // 1. Internal DTO -> External DTO Mapping
        AiServerRequest aiRequest = AiServerRequest.of(
                String.valueOf(request.getChatId()),
                String.valueOf(request.getPartyId()),
                String.valueOf(userId),
                request.getContent(),
                request.getTargetLang()
        );

        // 2. Call AI Server using WebClient
        AiServerResponse aiResponse = webClient.post()
                .uri(aiServerUrl + "/api/v1/chats/translate")
                .bodyValue(aiRequest)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            log.error("AI Server Error: {}", response.statusCode());
                            return Mono.error(new CustomException(GlobalErrorCode.TRANSLATION_API_ERROR));
                        })
                .bodyToMono(AiServerResponse.class)
                .block(); // Synchronous call for now (can be async later)

        if (aiResponse == null) {
            throw new CustomException(GlobalErrorCode.TRANSLATION_API_ERROR);
        }

        // 3. External DTO -> Client Response Mapping
        return TranslationResponse.of(
                request.getContent(),
                aiResponse.getTranslatedContent(),
                request.getTargetLang()
        );
    }
}
