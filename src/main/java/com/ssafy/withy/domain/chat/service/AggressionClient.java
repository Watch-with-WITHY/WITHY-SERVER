package com.ssafy.withy.domain.chat.service;

import com.ssafy.withy.grpc.slang.SlangFilterServiceGrpc;
import com.ssafy.withy.grpc.slang.CheckSlangRequest;
import com.ssafy.withy.grpc.slang.CheckSlangResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AggressionClient {

    @GrpcClient("service-a")
    private SlangFilterServiceGrpc.SlangFilterServiceBlockingStub slangStub;

    /**
     * gRPC 비속어 탐지 (Sync)
     * Limit: 100ms
     */
    public boolean checkAggression(String content, Integer userId, Integer partyId) {
        try {
            // 1. Request 생성
            CheckSlangRequest request = CheckSlangRequest.newBuilder()
                    .setUserId(String.valueOf(userId))
                    .setContent(content)
                    .setPartyId(String.valueOf(partyId))
                    .setChatId("temp-" + System.currentTimeMillis())
                    .build();

            // 2. gRPC 호출 (Blocking)
            CheckSlangResponse response = slangStub.checkSlang(request);

            // 3. 결과 분석
            if (!response.getIsSafe()) {
                log.warn("Aggressive content detected (User: {}, Reason: {})", userId, response.getReason());
                return true; // 공격성 있음
            }
            return false; // 안전
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE || e.getStatus().getCode() == Status.Code.UNIMPLEMENTED) {
                 log.warn("AI Service unavailable (Status: {}). Allowing message by Fail-Open policy.", e.getStatus().getCode());
                 return false; // Fail-open
            }
            log.error("gRPC call failed (Status: {}). Blocking message for safety.", e.getStatus().getCode(), e);
            return true; // Fail-closed for other errors (optional, usually fail-open is preferred for chat)
        } catch (Exception e) {
            log.error("Unknown error during Aggression Check", e);
            return false;
        }
    }
}
