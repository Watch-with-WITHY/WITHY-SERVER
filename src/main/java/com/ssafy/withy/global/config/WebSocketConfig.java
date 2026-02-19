package com.ssafy.withy.global.config;

import com.ssafy.withy.global.auth.handler.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 연결할 엔드포인트: /ws-stomp (HTTPS 가이드 준수)
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("http://localhost:3000", "https://watchwithwithy.vercel.app",
                        "https://3.36.95.236.nip.io", "https://www.netflix.com", "https://www.youtube.com",
                        "chrome-extension://*")
                .withSockJS(); // SockJS 지원 (WebSocket 미지원 브라우저 대비)
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 구독(수신)하는 요청의 prefix: /sub, /queue
        registry.enableSimpleBroker("/sub", "/queue");

        // 메시지를 발행(송신)하는 요청의 prefix: /pub
        registry.setApplicationDestinationPrefixes("/pub");
    }
}
