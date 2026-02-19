package com.ssafy.withy.domain.chat.service;

import com.ssafy.withy.grpc.slang.CheckSlangRequest;
import com.ssafy.withy.grpc.slang.CheckSlangResponse;
import com.ssafy.withy.grpc.slang.SlangFilterServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AggressionClientTest {

    @Mock
    private SlangFilterServiceGrpc.SlangFilterServiceBlockingStub slangStub;

    @InjectMocks
    private AggressionClient aggressionClient;

    @Test
    @DisplayName("gRPC 비속어 탐지: 욕설이 포함된 경우 true 반환")
    void checkAggression_Aggressive() {
        // given
        CheckSlangResponse mockResponse = CheckSlangResponse.newBuilder()
                .setIsSafe(false)
                .setReason("욕설 감지")
                .build();

        given(slangStub.checkSlang(any(CheckSlangRequest.class))).willReturn(mockResponse);

        // when
        boolean result = aggressionClient.checkAggression("나쁜말", 1, 100);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("gRPC 비속어 탐지: 안전한 메시지는 false 반환")
    void checkAggression_Safe() {
        // given
        CheckSlangResponse mockResponse = CheckSlangResponse.newBuilder()
                .setIsSafe(true)
                .setReason("Safe")
                .build();

        given(slangStub.checkSlang(any(CheckSlangRequest.class))).willReturn(mockResponse);

        // when
        boolean result = aggressionClient.checkAggression("안녕하세요", 1, 100);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("gRPC 장애 발생 시 Fail-Open (false 반환)")
    void checkAggression_FailOpen() {
        // given
        given(slangStub.checkSlang(any(CheckSlangRequest.class)))
                .willThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        // when
        boolean result = aggressionClient.checkAggression("메시지", 1, 100);

        // then
        assertThat(result).isFalse(); // Fail-open: 장애 시 통과
    }
}
