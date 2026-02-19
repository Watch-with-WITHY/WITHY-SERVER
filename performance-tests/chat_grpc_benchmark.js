
import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';

// 1. gRPC 클라이언트 생성
const client = new grpc.Client();

// 2. Proto 파일 로드 (상대 경로 주의)
// 로컬 실행 시 경로: src/main/proto/chat.proto
// Docker/CI 실행 시 경로를 맞춰주세요.
client.load(['../src/main/proto'], 'chat.proto');

export const options = {
    // 부하 테스트 설정
    stages: [
        { duration: '30s', target: 100 }, // 30초 동안 사용자 100명으로 증가
        { duration: '1m', target: 100 },  // 1분 동안 사용자 100명 유지
        { duration: '30s', target: 0 },   // 30초 동안 사용자 0명으로 감소
    ],
};

export default function () {
    // 3. gRPC 연결 (AI 서버 - Ngrok)
    // application-local.yml에 설정된 주소 사용
    client.connect('inger-sacred-diffusedly.ngrok-free.dev:443', {
        plaintext: false, // TLS(HTTPS) 사용 (443 포트)
    });

    const data = {
        user_id: 'user_123',
        content: 'This is a test message checking performance',
        chat_id: 'chat_room_1',
        party_id: 'party_1',
    };

    // 4. gRPC 메서드 호출
    const start = Date.now();
    const response = client.invoke('chat.SlangFilterService/CheckSlang', data);
    const duration = Date.now() - start;

    // 5. 검증
    check(response, {
        'status is OK': (r) => r && r.status === grpc.StatusOK,
        'latency < 200ms': (r) => duration < 200,
    });

    client.close();
    sleep(1);
}
