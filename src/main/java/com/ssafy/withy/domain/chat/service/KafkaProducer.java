package com.ssafy.withy.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "spoiler-check";

    /**
     * Redis 버퍼에서 꺼낸 메시지 묶음(List<String:JSON>)을 Kafka로 전송한다.
     * 메시지는 AI 서버가 소비(Consume)하여 스포일러 분석을 수행한다.
     */
    public void sendSpoilerCheck(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 묶음 그대로 보낼 수도 있고, 하나씩 보낼 수도 있음.
        // 여기서는 논리적으로 "5개 1세트"를 처리한다고 가정하고, Consumer가 List를 받을 수 있게 하거나
        // 편의상 루프를 돌며 개별 전송할 수 있음.
        // 아키텍처 상 '배치 처리' 느낌이므로 하나하나 produce 하되, key를 맞춰서 순서를 보장하거나
        // Consumer 측에서 windowing을 할 수도 있음.
        // 현재 로직: 루프 돌며 전송 (간단 구현)
        for (String msg : messages) {
            // 비동기 전송
            kafkaTemplate.send(TOPIC, msg)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send message to Kafka: {}", msg, ex);
                        }
                        // 성공 로그는 너무 많을 수 있으니 생략 또는 debug
                    });
        }
        log.info("Produced {} messages to Kafka topic: {}", messages.size(), TOPIC);
    }
}
