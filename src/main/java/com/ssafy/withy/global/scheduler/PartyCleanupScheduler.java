package com.ssafy.withy.global.scheduler;

import com.ssafy.withy.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyCleanupScheduler {

    private final PartyService partyService;

    // 10분마다 추가
    @Scheduled(cron = "0 0/10 * * * *")
    public void deleteExpiredParties() {
        log.info("🧹 [Scheduler] 만료된 비활성 파티 정리를 시작합니다...");

        try {
            int deletedCount = partyService.cleanupExpiredParties();
            if (deletedCount > 0) {
                log.info("✨ [Scheduler] 총 {}개의 만료된 파티를 삭제했습니다.", deletedCount);
            }
        } catch (Exception e) {
            log.error("🚨 [Scheduler] 파티 정리 중 오류 발생: {}", e.getMessage());
        }
    }
}
