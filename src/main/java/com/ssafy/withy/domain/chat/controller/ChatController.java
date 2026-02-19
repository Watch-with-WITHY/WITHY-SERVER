package com.ssafy.withy.domain.chat.controller;

import com.ssafy.withy.domain.chat.dto.ChatRequest;
import com.ssafy.withy.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

        private final ChatService chatService;
        private final com.ssafy.withy.domain.chat.service.AggressionClient aggressionClient;

        // 클라이언트 전송 경로: /pub/chat/message
        @MessageMapping("/chat/message")
        public void sendMessage(@Payload ChatRequest chatRequest) {
                log.info("Received message: {}", chatRequest.getContent());
                chatService.processMessage(chatRequest);
        }

        // 예외 처리: 공격성 메시지 등 에러가 나면 보낸 사람에게만 에러 전송
        @MessageExceptionHandler
        @SendToUser("/queue/errors")
        public String handleException(com.ssafy.withy.global.error.exception.CustomException e) {
                log.error("Chat error: {}", e.getErrorCode().getMessage(), e);
                return e.getErrorCode().getMessage();
        }

        @MessageExceptionHandler
        @SendToUser("/queue/errors")
        public String handleException(IllegalArgumentException e) {
                log.error("Chat error: {}", e.getMessage(), e);
                return e.getMessage();
        }

    // 채팅 삭제 API
    @Operation(summary = "채팅 메시지 삭제", description = """
            - 채팅 메시지를 삭제합니다.
            - 본인이 작성한 메시지이거나, 파티 방장(HOST) 또는 매니저(MANAGER) 권한이 있어야 삭제할 수 있습니다.
            - 삭제된 메시지는 데이터베이스에서 완전히 제거되지 않고 is_deleted 플래그가 true로 설정되며(Soft Delete)
            - WebSocket을 통해 해당 파티의 모든 구독자에게 실시간으로 삭제 상태가 전파됩니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅 메시지 삭제 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.ssafy.withy.global.common.response.ApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 - 본인의 메시지가 아니며 방장/매니저 권한도 없음", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "채팅 메시지를 찾을 수 없음", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/api/chat/messages/{chatId}")
    @ResponseBody
    public ResponseEntity<com.ssafy.withy.global.common.response.ApiResponse<String>> deleteChat(
            @Parameter(description = "삭제할 채팅 메시지의 ID", required = true, example = "123") @PathVariable Integer chatId,
            @AuthenticationPrincipal com.ssafy.withy.global.auth.dto.CustomUserDetails userDetails) {

        Integer userId = userDetails.getUserId();
        chatService.deleteChat(chatId, userId);

        // 성공 응답 (API Spec에 맞춤)
        return ResponseEntity.ok(
                com.ssafy.withy.global.common.response.ApiResponse.success(
                        com.ssafy.withy.global.common.code.GlobalSuccessCode.DELETE_CHAT_SUCCESS,
                        "채팅 메시지가 성공적으로 삭제되었습니다."));
    }
    // --- Performance Test Endpoint (Temporary) ---
    @PostMapping("/api/test/slang-check")
    @ResponseBody
    public ResponseEntity<Boolean> checkSlangRest(@RequestBody ChatRequest request) {
        // gRPC 로직을 REST로 래핑하여 호출 (프로토콜 오버헤드 비교용)
        boolean isAggressive = aggressionClient.checkAggression(
                request.getContent(),
                Math.toIntExact(request.getUserId()),
                Math.toIntExact(request.getPartyId())
        );
        return ResponseEntity.ok(isAggressive);
    }
}
