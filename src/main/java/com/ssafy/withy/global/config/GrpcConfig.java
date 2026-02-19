package com.ssafy.withy.global.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// NOTE: 실제 빌드 시 protoc로 생성된 코드(AggressionCheckServiceGrpc)가 필요하지만,
// 현재 환경에서는 생성할 수 없으므로 주석 처리 또는 Mock 구조로 잡아둡니다.
// import com.ssafy.withy.grpc.aggression.AggressionCheckServiceGrpc;

@Configuration
public class GrpcConfig {

    @Value("${grpc.server.host:localhost}")
    private String grpcHost;

    @Value("${grpc.server.port:50051}")
    private int grpcPort;

    @Bean
    public ManagedChannel managedChannel() {
        // Plaintext 채널 (운영 환경에선 TLS 적용 권장)
        // 네고시에이션 비용 없이 빠르게 연결
        return ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
    }

    // @Bean
    // public AggressionCheckServiceGrpc.AggressionCheckServiceBlockingStub
    // aggressionStub(ManagedChannel channel) {
    // // BlockingStub: 동기 호출 (0.1초 내 응답이 목표이므로 단순 동기 호출이 디버깅에 유리)
    // return AggressionCheckServiceGrpc.newBlockingStub(channel);
    // }
}
