package com.ssafy.withy.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.withy.domain.chat.dto.TranslationRequest;
import com.ssafy.withy.domain.chat.dto.TranslationResponse;
import com.ssafy.withy.domain.chat.service.TranslationService;
import com.ssafy.withy.global.auth.dto.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatTranslationControllerTest {

    @InjectMocks
    private ChatTranslationController chatTranslationController;

    @Mock
    private TranslationService translationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("성공: 채팅 번역 요청 시 번역된 결과를 반환한다.")
    void translate_Success() throws Exception {
        // given
        TranslationRequest request = TranslationRequest.builder()
                .chatId(100)
                .partyId(1)
                .content("Hello")
                .targetLang("ko")
                .build();

        TranslationResponse response = TranslationResponse.builder()
                .originalContent("Hello")
                .translatedContent("안녕하세요")
                .targetLang("ko")
                .build();

        // User & CustomUserDetails 생성
        com.ssafy.withy.domain.user.entity.User user = com.ssafy.withy.domain.user.entity.User.builder()
                .id(10)
                .email("test@test.com")
                .password("password")
                .role(com.ssafy.withy.domain.user.entity.Role.USER)
                .isActive(true)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Custom Argument Resolver: @AuthenticationPrincipal 파라미터가 보이면 우리가 만든 userDetails를 반환
        mockMvc = MockMvcBuilders.standaloneSetup(chatTranslationController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(CustomUserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return userDetails;
                    }
                })
                .build();

        // Service Mocking
        given(translationService.translate(any(TranslationRequest.class), any(Long.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/chats/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("번역에 성공했습니다."))
                .andExpect(jsonPath("$.data.translatedContent").value("안녕하세요"));
    }
}
