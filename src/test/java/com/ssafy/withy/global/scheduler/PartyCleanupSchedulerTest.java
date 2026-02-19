package com.ssafy.withy.global.scheduler;

import com.ssafy.withy.domain.party.service.PartyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PartyCleanupSchedulerTest {

    @InjectMocks
    private PartyCleanupScheduler partyCleanupScheduler;

    @Mock
    private PartyService partyService;

    @Test
    @DisplayName("스케줄러가 실행되면 만료된 파티 정리 서비스 로직이 호출된다.")
    void deleteExpiredParties_Success() {
        // given
        // 서비스가 "5개 삭제함"이라고 리턴한다고 가정
        given(partyService.cleanupExpiredParties()).willReturn(5);

        // when
        // 스케줄러 메서드 직접 호출 (Cron 타이밍 테스트 아님)
        partyCleanupScheduler.deleteExpiredParties();

        // then
        // 1. 서비스 메서드가 정확히 1번 호출되었는지 검증
        then(partyService).should(times(1)).cleanupExpiredParties();
    }

    @Test
    @DisplayName("서비스 실행 중 예외가 발생해도 스케줄러는 중단되지 않고 로그를 남긴다.")
    void deleteExpiredParties_Exception() {
        // given
        // 서비스가 예외를 던짐
        given(partyService.cleanupExpiredParties()).willThrow(new RuntimeException("DB Error"));

        // when
        // 예외가 발생해도 스케줄러 메서드 자체는 터지지 않고 try-catch로 넘어가야 함
        partyCleanupScheduler.deleteExpiredParties();

        // then
        // 1. 호출은 시도했는지 확인
        then(partyService).should(times(1)).cleanupExpiredParties();
        // (로그 확인은 어렵지만, 테스트가 Error로 끝나지 않으면 성공)
    }
}
