import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// 메트릭 정의
const pollingLatency = new Trend('polling_latency'); // 폴링 지연 시간 (단방향 요청 RTT 포함)
const requestRate = new Counter('request_rate'); // 초당 요청 수

const BASE_URL = 'http://localhost:8080';

export const options = {
    scenarios: {
        extension_polling: {
            executor: 'shared-iterations',
            vus: 50, // 1 Host + 49 Guests
            iterations: 2500, // (50 users * 1 request/sec * 50 sec roughly)
            maxDuration: '1m', // 1분간 테스트
        },
    },
};

function randomString(length) {
    const charset = 'abcdefghijklmnopqrstuvwxyz0123456789';
    let res = '';
    for (let i = 0; i < length; i++) {
        res += charset[Math.floor(Math.random() * charset.length)];
    }
    return res;
}

// 1. Setup: 유저 생성 및 파티 생성
export function setup() {
    const uniqueSuffix = randomString(5);

    const hostUser = {
        email: `host_poll_${uniqueSuffix}@test.com`,
        password: 'Test1234!',
        nickname: `HostUserPoll_${uniqueSuffix}`
    };

    let res = http.post(`${BASE_URL}/api/v1/auth/signup`, JSON.stringify(hostUser), { headers: { 'Content-Type': 'application/json' } });
    if (res.status !== 200 && res.status !== 201) {
        console.error(`Host Signup Failed: ${res.status} ${res.body}`);
    }

    const hostLoginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify(hostUser), { headers: { 'Content-Type': 'application/json' } });
    if (hostLoginRes.status !== 200) {
        console.error(`Host Login Failed: ${hostLoginRes.status} ${hostLoginRes.body}`);
        throw new Error('Host login failed');
    }
    const hostToken = hostLoginRes.json('data.accessToken');

    const partyPayload = JSON.stringify({
        title: 'Polling Test Party',
        contentId: 999,
        contentTitle: 'Test Movie',
        platform: 'OTT',
        maxParticipants: 100,
        isPrivate: false,
        password: null,
        scheduledActiveTime: new Date(Date.now() + 86400000).toISOString() // 24시간 뒤 (TimeZone 이슈 회피)
    });

    const partyRes = http.post(`${BASE_URL}/api/v1/parties`, partyPayload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${hostToken}`
        }
    });

    let partyId;
    if (partyRes.status === 200 || partyRes.status === 201) {
        partyId = partyRes.json('data.partyId'); // Response structure: data: { partyId: 3 }
        if (!partyId) partyId = partyRes.json('data'); // Fallback if data is just ID
    } else {
        console.error(`Party Creation Failed: ${partyRes.status} ${partyRes.body}`);
        throw new Error('Party creation failed');
    }

    const guestTokens = [];
    for (let i = 0; i < 49; i++) {
        const guestSuffix = randomString(5);
        const guestUser = {
            email: `guest_poll_${i}_${guestSuffix}@test.com`,
            password: 'Test1234!',
            nickname: `GuestPoll${i}_${guestSuffix}`
        };
        http.post(`${BASE_URL}/api/v1/auth/signup`, JSON.stringify(guestUser), { headers: { 'Content-Type': 'application/json' } });
        const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify(guestUser), { headers: { 'Content-Type': 'application/json' } });
        if (res.status === 200) {
            guestTokens.push(res.json('data.accessToken'));
        } else {
            console.error(`Guest Login Failed: ${res.status}`);
        }
    }

    // Guests need to join the party (handled implicitly by API or need explicit join? 
    // WebSocket controller auto-joined GUESTs. Here accessing GET might not check participant strictly or controller handles it.
    // Ideally we join them. For simplicity, we assume GET endpoint creates participant or checks loosely.)

    return { hostToken, guestTokens, partyId };
}

export default function (data) {
    const isHost = (__VU === 1);
    const token = isHost ? data.hostToken : data.guestTokens[__VU - 2];
    const partyId = data.partyId;

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };

    if (isHost) {
        // HOST: Update State (POST)
        const payload = JSON.stringify({
            commandType: 'SEEK',
            currentPosition: Math.random() * 1000,
            isPlaying: true,
            partyUrl: 'http://test.com',
            timestamp: Date.now(),
            playbackRate: 1.0
        });

        const res = http.post(`${BASE_URL}/api/v1/parties/${partyId}/state/polling`, payload, { headers: headers });
        check(res, { 'Host update success': (r) => r.status === 200 });
        requestRate.add(1);

    } else {
        // GUEST: Poll State (GET)
        const start = Date.now();
        const res = http.get(`${BASE_URL}/api/v1/parties/${partyId}/state/polling`, { headers: headers });
        const end = Date.now();

        check(res, { 'Guest poll success': (r) => r.status === 200 });

        if (res.status === 200) {
            pollingLatency.add(end - start);
            requestRate.add(1);
        }
    }

    sleep(1); // 1초 대기 (Polling Interval)
}
