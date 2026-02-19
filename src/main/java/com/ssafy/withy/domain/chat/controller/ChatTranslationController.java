package com.ssafy.withy.domain.chat.controller;

import com.ssafy.withy.domain.chat.dto.TranslationRequest;
import com.ssafy.withy.domain.chat.dto.TranslationResponse;
import com.ssafy.withy.domain.chat.service.TranslationService;
import com.ssafy.withy.global.auth.dto.CustomUserDetails;
import com.ssafy.withy.global.common.code.GlobalSuccessCode;
import com.ssafy.withy.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat Translation", description = "실시간 채팅 번역 API")
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatTranslationController {

    private final TranslationService translationService;

    @Operation(summary = "채팅 메시지 번역 요청", description = """
            - 사용자의 채팅 메시지를 AI 서버를 통해 번역합니다.
            - 서버 간 통신(Server-to-Server)으로 외부 AI API를 호출합니다.
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "번역 성공",
                    content = @Content(schema = @Schema(implementation = TranslationResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI 서버 연동 실패", content = @Content)
    })
    @PostMapping("/translate")
    public ResponseEntity<ApiResponse<TranslationResponse>> translate(
            @Valid @RequestBody TranslationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId().longValue(); // Assuming userId is Integer internally but service might use Long/Int depending on User entity.
        // Wait, CustomUserDetails.getUserId() returns Integer usually. 
        // User entity uses Integer. 
        // TranslationService.translate takes Long? I defined it as Long in Implementation thought, but User entity uses Integer.
        // Let's check TranslationService again. I wrote it as Long userId previously? 
        // No, I wrote `public TranslationResponse translate(TranslationRequest request, Long userId)` in the write_to_file tool.
        // But User entity uses Integer. I should cast or change Service to Integer.
        // Safer to change Service to Integer to match Architecture. 
        // Actually, let's cast here to match the Service I just wrote, or refactor Service.
        // Refactoring Service to Integer is better. But I already wrote it.
        // Let's modify Service to Integer in the next step or just cast here if it's compatible.
        // I'll stick to what I wrote: translate takes Long. So I cast here: userDetails.getUserId().longValue().
        
        TranslationResponse response = translationService.translate(request, userId);

        return ResponseEntity.ok(ApiResponse.success(
                GlobalSuccessCode.TRANSLATE_CHAT_SUCCESS,
                response
        ));
    }
}
