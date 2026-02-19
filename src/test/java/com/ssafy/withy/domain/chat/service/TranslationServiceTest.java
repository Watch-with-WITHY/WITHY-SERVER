package com.ssafy.withy.domain.chat.service;

import com.ssafy.withy.domain.chat.dto.AiServerResponse;
import com.ssafy.withy.domain.chat.dto.TranslationRequest;
import com.ssafy.withy.domain.chat.dto.TranslationResponse;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @InjectMocks
    private TranslationService translationService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Test
    @DisplayName("성공: AI 서버 번역 요청이 성공하면 번역된 결과를 반환한다.")
    void translate_Success() {
        // given
        ReflectionTestUtils.setField(translationService, "aiServerUrl", "http://ai-server");

        TranslationRequest request = TranslationRequest.builder()
                .chatId(100)
                .partyId(1)
                .content("Hello")
                .targetLang("ko")
                .build();
        Long userId = 10L;

        AiServerResponse mockResponse = AiServerResponse.builder()
                .chatId("100")
                .partyId("1")
                .userId("10")
                .translatedContent("안녕하세요")
                .build();

        // WebClient Mocking Chain
        given(webClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.bodyValue(any())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(Predicate.class), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(AiServerResponse.class)).willReturn(Mono.just(mockResponse));

        // when
        TranslationResponse response = translationService.translate(request, userId);

        // then
        assertThat(response.getOriginalContent()).isEqualTo("Hello");
        assertThat(response.getTranslatedContent()).isEqualTo("안녕하세요");
        assertThat(response.getTargetLang()).isEqualTo("ko");
    }

    @Test
    @DisplayName("실패: AI 서버 응답이 null이면 예외가 발생한다.")
    void translate_Fail_NullResponse() {
        // given
        ReflectionTestUtils.setField(translationService, "aiServerUrl", "http://ai-server");

        TranslationRequest request = TranslationRequest.builder()
                .chatId(100)
                .partyId(1)
                .content("Hello")
                .targetLang("ko")
                .build();
        Long userId = 10L;

        // WebClient Mocking Chain
        given(webClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.bodyValue(any())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(Predicate.class), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(AiServerResponse.class)).willReturn(Mono.empty()); // Return empty

        // when & then
        assertThatThrownBy(() -> translationService.translate(request, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.TRANSLATION_API_ERROR);
    }
}
