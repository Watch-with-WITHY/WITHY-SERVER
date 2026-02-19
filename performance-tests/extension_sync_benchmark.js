import ws from 'k6/ws';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// 메트릭 정의
const syncLatency = new Trend('sync_latency'); // 동기화 지연 시간
const messageRate = new Counter('message_rate'); // 초당 메시지 처리 수

// 설정
const BASE_URL = 'http://localhost:8080';
const WS_URL = 'ws://localhost:8080/ws-stomp/websocket'; // STOMP 엔드포인트 확인 필요

export const options = {
    scenarios: {
        extension_sync: {
            executor: 'shared-iterations',
            vus: 50, // 1 Host + 49 Guests
            iterations: 50, // 각자 연결 유지
            maxDuration: '1m', // 1분간 테스트
        },
    },
};

// 유틸리티: 랜덤 문자열
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
        email: `host_sync_${uniqueSuffix}@test.com`,
        password: 'Test1234!',
        nickname: `HostSync_${uniqueSuffix}`
    };

    // 호스트 가입 & 로그인
    let res = http.post(`${BASE_URL}/api/v1/auth/signup`, JSON.stringify(hostUser), { headers: { 'Content-Type': 'application/json' } });
    if (res.status !== 200 && res.status !== 201) {
        console.error(`Host Signup Failed: ${res.status} ${res.body}`);
    }

    const hostLoginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify(hostUser), { headers: { 'Content-Type': 'application/json' } });
    if (hostLoginRes.status !== 200) {
        console.error(`Host Login Failed: ${hostLoginRes.status}`);
        throw new Error('Host login failed');
    }
    const hostToken = hostLoginRes.json('data.accessToken');

    // 파티 생성
    const partyPayload = JSON.stringify({
        title: 'Performance Test Party',
        contentId: 999, // 사전에 존재하는 컨텐츠 ID (없으면 에러날 수 있음, 확인 필요)
        contentTitle: 'Test Movie', // getOrCreateContent용
        platform: 'OTT',
        maxParticipants: 100,
        isPrivate: false,
        password: null,
        scheduledActiveTime: new Date(Date.now() + 86400000).toISOString() // 24시간 뒤
    });

    const partyRes = http.post(`${BASE_URL}/api/v1/parties`, partyPayload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${hostToken}`
        }
    });

    // 파티 생성 실패 시 대응 (컨텐츠 999가 없을 경우 대비)
    let partyId;
    if (partyRes.status === 200 || partyRes.status === 201) {
        partyId = partyRes.json('data.partyId');
        if (!partyId) partyId = partyRes.json('data');
    } else {
        console.error(`Party Creation Failed: ${partyRes.status} ${partyRes.body}`);
        throw new Error('Party creation failed');
    }

    // 게스트 가입 & 로그인 (49명)
    const guestTokens = [];
    for (let i = 0; i < 49; i++) {
        const guestSuffix = randomString(5);
        const guestUser = {
            email: `guest_sync_${i}_${guestSuffix}@test.com`,
            password: 'Test1234!',
            nickname: `GuestSync${i}_${guestSuffix}`
        };
        http.post(`${BASE_URL}/api/v1/auth/signup`, JSON.stringify(guestUser), { headers: { 'Content-Type': 'application/json' } });
        const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify(guestUser), { headers: { 'Content-Type': 'application/json' } });
        if (res.status === 200) {
            guestTokens.push(res.json('data.accessToken'));
        } else {
            console.error(`Guest Login Failed: ${res.status}`);
        }
    }

    return { hostToken, guestTokens, partyId };
}

export default function (data) {
    const isHost = (__VU === 1);
    const token = isHost ? data.hostToken : data.guestTokens[__VU - 2];
    const partyId = data.partyId;

    if (!token) {
        console.error(`VU ${__VU} missing token`);
        return;
    }

    const params = {
        tags: { role: isHost ? 'HOST' : 'GUEST' }
    };

    const response = ws.connect(WS_URL, params, function (socket) {
        // STOMP Connect Frame
        socket.on('open', function open() {
            // [중요] WebSocket 헤더가 아닌 STOMP 프레임 헤더에 토큰 포함
            socket.send(`CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\nAuthorization:Bearer ${token}\n\n\0`);
        });

        socket.on('message', function (msg) {
            if (msg.startsWith('CONNECTED')) {
                // Subscribe after connection
                socket.send(`SUBSCRIBE\nid:sub-0\ndestination:/sub/party/${partyId}/extension\n\n\0`);

                // Handshake (Optional for test, but mimicking real flow)
                socket.send(`SEND\ndestination:/pub/party/${partyId}/extension/handshake\n\n\0`);

                // HOST ONLY logic
                if (isHost) {
                    socket.setInterval(() => {
                        const payload = JSON.stringify({
                            commandType: 'SEEK',
                            currentPosition: Math.random() * 1000,
                            isPlaying: true,
                            partyUrl: 'http://test.com',
                            timestamp: Date.now(), // Host timestamp
                            playbackRate: 1.0
                        });
                        socket.send(`SEND\ndestination:/pub/party/${partyId}/extension/state\ncontent-type:application/json\n\n${payload}\0`);
                    }, 1000); // 1초마다 전송
                }
            } else if (msg.startsWith('MESSAGE')) {
                // GUEST Logic: Measure Latency
                if (!isHost) {
                    const body = msg.split('\n\n')[1].replace(/\0/g, '');
                    const json = JSON.parse(body);

                    if (json.timestamp) {
                        const now = Date.now();
                        const latency = now - json.timestamp;
                        syncLatency.add(latency);
                        messageRate.add(1);
                    }
                }
            }
        });

        socket.on('close', () => console.log(`VU ${__VU} disconnected`));
        socket.on('error', (e) => console.error(`VU ${__VU} error:`, e));

        // 30초 후 종료
        socket.setTimeout(() => {
            socket.close();
        }, 30000);
    });

    check(response, { 'status is 101': (r) => r && r.status === 101 });
}
