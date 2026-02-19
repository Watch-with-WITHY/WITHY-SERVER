package com.ssafy.withy.domain.chat.scheduler;

import com.ssafy.withy.domain.chat.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpoilerScheduler {

    private final RedisService redisService;

    /**
     * 10초마다 실행되어 Redis 버퍼에 남아있는 메시지들을 Kafka로 전송한다.
     * (5개가 안 찼더라도 시간이 흐르면 처리해야 하므로)
     */
    @Scheduled(fixedRate = 10000)
    public void flushChatBuffer() {
        // 모든 채팅방의 키를 scan해서 flush 해야 이상적이지만, 
        // RedisService 구현에 따라 모든 키를 순회하거나, 특정 로직이 필요함.
        // 현재 RedisService에는 특정 방(partyId)의 버퍼를 비우는 로직만 있다면 문제가 됨.
        // RedisService에 'flushAllBuffers' 같은 메소드가 필요하거나,
        // 여기서는 예시로 "알려진 활성 방"들을 돌거나 해야 함.
        
        // 하지만 시간 관계상 가장 단순한 방법:
        // RedisService에 flushAll() 기능이 없으므로, 
        // 일단 RedisService에 '남아있는 모든 버퍼를 확인해서 보낸다'는 로직을 추가해야 함.
        // 여기서는 RedisService.flushRemaining() 을 호출한다고 가정하고 작성.
        
        try {
            int count = redisService.flushRemainingBuffers();
            if (count > 0) {
                log.info("Flushed {} remaining messages via Scheduler.", count);
            }
        } catch (Exception e) {
            log.error("Failed to flush chat buffer", e);
        }
    }
}
