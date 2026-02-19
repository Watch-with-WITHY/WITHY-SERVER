# K6 성능 테스트 가이드

이 문서는 gRPC 기반 채팅(비속어 필터링) 서비스의 성능을 측정하고, REST API와 비교하기 위한 가이드입니다.

## 1. K6 설치

K6는 Go 언어로 개발된 오픈소스 부하 테스트 도구입니다.

### Windows (Chocolatey 사용)
```powershell
choco install k6
```

### Windows (Winget 사용)
```powershell
winget install k6
```

### 수동 설치 (MSI)
[K6 다운로드 페이지](https://k6.io/docs/get-started/installation/)에서 Windows용 MSI 설치 파일을 다운로드하여 설치하세요.

설치 확인:
```powershell
k6 version
```

## 2. gRPC 성능 테스트 실행

`chat_grpc_benchmark.js` 스크립트는 `SlangFilterService`의 `CheckSlang` 메서드를 호출하여 성능을 측정합니다.

### 사전 준비
1. 로컬에서 백엔드 서버를 실행해야 합니다. (gRPC 포트 확인 필요, 기본 `9090`으로 설정됨)
2. `chat_grpc_benchmark.js` 파일 내 `client.connect('localhost:9090', ...)` 부분의 주소와 포트가 맞는지 확인하세요.

### 실행 명령어
`performance-tests` 폴더 내에서 실행하세요:

```powershell
k6 run chat_grpc_benchmark.js
```

### 주요 지표 설명
- **http_req_duration (gRPC는 duration)**: 요청 처리 시간 (낮을수록 좋음)
- **grpc_req_duration**: gRPC 요청 처리 시간
- **iterations**: 테스트 반복 횟수 (처리량)

## 3. REST API 비교 테스트 (옵션)

REST와의 비교를 위해서는 동일한 로직을 수행하는 REST API 엔드포인트가 필요합니다.
만약 `POST /api/v1/slang-check`와 같은 엔드포인트를 만드셨다면, 아래와 같이 K6 스크립트를 작성하여 비교할 수 있습니다.

**chat_rest_benchmark.js 예시:**
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 10 },
    { duration: '30s', target: 0 },
  ],
};

export default function () {
  const url = 'http://localhost:8080/api/v1/slang-check';
  const payload = JSON.stringify({
    user_id: 'user_123',
    content: 'test content',
    chat_id: 'chat_room_1',
    party_id: 'party_1',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(url, payload, params);
  check(res, { 'status was 200': (r) => r.status == 200 });
  sleep(1);
}
```

두 스크립트를 각각 실행하여 **요청 처리 시간(duration)**과 **초당 처리량(RPS)**을 비교해보세요. 일반적으로 gRPC가 더 낮은 지연 시간과 높은 처리량을 보일 것입니다.
