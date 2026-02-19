package com.ssafy.withy.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.withy.domain.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaProducer kafkaProducer; // Kafka로 보내는 역할

    private static final String SCRIPT_PATH = "scripts/atomic-chat-buffer.lua";

    /**
     * 채팅 메시지를 Redis 버퍼에 저장하고, 5개가 차면 Kafka로 전송한다.
     * 이 모든 과정은 Lua Script를 통해 원자적(Atomic)으로 수행된다.
     */
    public void bufferAndPublish(ChatResponse chatResponse) {
        String key = "chat:buffer:" + chatResponse.getPartyId();
        
        // 활성화된 채팅방 목록에 추가 (Scheduler가 스캔하기 위함)
        redisTemplate.opsForSet().add("active:chat:buffers", key);

        try {
            String value = objectMapper.writeValueAsString(chatResponse);

            // Lua Script 로드
            DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(SCRIPT_PATH)));
            redisScript.setResultType(List.class);

            // 스크립트 실행
            List<String> result = redisTemplate.execute(redisScript, Collections.singletonList(key), value);

            // 결과가 있으면 (5개가 찼으면) Kafka로 전송
            if (result != null && !result.isEmpty()) {
                log.info("Redis buffer full ({} messages). Sending to Kafka.", result.size());
                kafkaProducer.sendSpoilerCheck(result);
                // 비워졌으므로 활성 목록에서 제거할 수도 있으나, 잦은 추가/삭제를 피하기 위해 스케줄러에서 처리하거나 유지.
                // 여기서는 유지 (채팅이 계속 오면 어차피 추가됨).
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat message for Redis", e);
        } catch (Exception e) {
            log.error("Redis operation failed", e);
        }
    }

    /**
     * 스포일러 스케줄러가 호출.
     * 활성화된 버퍼들을 순회하며 남아있는 메시지들을 모두 강제로 꺼내서 Kafka로 보낸다.
     */
    public int flushRemainingBuffers() {
        // 1. 활성 버퍼 키 조회
        java.util.Set<String> keys = redisTemplate.opsForSet().members("active:chat:buffers");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        int totalFlushed = 0;
        for (String key : keys) {
            // 2. 각 키에 대해 LPOP ALL (혹은 LRANGE + DEL)
            // 여기서는 원자성을 위해 Lua Script를 쓰거나, 단순 트랜잭션 사용.
            // 단순하게: LRANGE -> DEL -> Send (동시성 이슈 가능성 있으나, Scheduler는 싱글스레드고 BufferInput은 Atomic Lua라 
            // 엇갈릴 수 있음. 안전하게 Lua로 'GET ALL AND DEL' 처리)
            
            // 기존 스크립트 재활용 불가 (기존: len>=5 check). 
            // 새 스크립트 없이 Java code로 처리 시: Watch -> Multi -> Exec
            // 또는 간단히: rename key to temp -> get temp -> del temp (but buffer keeps coming).
            
            // 가장 안전: 'get_and_del.lua' 필요. 없으면 여기서 간단 구현.
            // 여기서는 "남은거 다 줘" 로직.
            List<String> messages = popAllMessages(key);
            
            if (messages != null && !messages.isEmpty()) {
                kafkaProducer.sendSpoilerCheck(messages);
                totalFlushed += messages.size();
            } else {
                // 비어있으면 활성 셋에서 제거 (Cleanup)
                redisTemplate.opsForSet().remove("active:chat:buffers", key);
            }
        }
        return totalFlushed;
    }

    private List<String> popAllMessages(String key) {
        // Lua script to atomically get all and delete
        String script = "local items = redis.call('LRANGE', KEYS[1], 0, -1); redis.call('DEL', KEYS[1]); return items;";
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(List.class);
        return redisTemplate.execute(redisScript, Collections.singletonList(key));
    }

    /**
     * 영화 줄거리(Overview) 캐싱 조회 (Cache-Aside Pattern)
     * 1. Redis 조회 (party:plot:{partyId})
     * 2. 없으면 DB 조회 (Supplier) 후 Redis 저장 (TTL 1시간)
     */
    public String getCachedPlot(Integer partyId, java.util.function.Supplier<String> plotSupplier) {
        String key = "party:plot:" + partyId;
        
        try {
            String cachedPlot = redisTemplate.opsForValue().get(key);
            if (cachedPlot != null) {
                log.debug("Cache Hit for Party Plot: {}", partyId);
                return cachedPlot;
            }
        } catch (Exception e) {
            log.error("Redis get failed for plot", e);
        }

        // Cache Miss -> DB Query
        String plot = plotSupplier.get();
        
        if (plot != null) {
            try {
                redisTemplate.opsForValue().set(key, plot, java.time.Duration.ofHours(1));
                log.debug("Cache Miss. Fetched from DB and cached for Party: {}", partyId);
            } catch (Exception e) {
                log.error("Redis set failed for plot", e);
            }
        }
        
        return plot;
    }
}
