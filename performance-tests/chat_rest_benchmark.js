
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    // gRPC와 동일한 부하 시나리오
    stages: [
        { duration: '30s', target: 100 }, // 30초 동안 사용자 100명으로 증가
        { duration: '1m', target: 100 },  // 1분 동안 사용자 100명 유지
        { duration: '30s', target: 0 },   // 30초 동안 사용자 0명으로 감소
    ],
};

export default function () {
    // 포트 번호 확인 필요 (Spring Web 기본 8080)
    const url = 'http://localhost:8080/api/test/slang-check';

    // JSON Payload
    const payload = JSON.stringify({
        userId: 1,
        content: 'This is a test message for REST benchmark',
        partyId: 1,
        type: 'TEXT'
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(url, payload, params);

    // 검증
    check(res, {
        'status is 200': (r) => r.status === 200,
        'latency < 200ms': (r) => r.timings.duration < 200,
    });

    sleep(1);
}
