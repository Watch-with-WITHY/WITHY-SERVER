# 🚀 K6 성능 테스트 완벽 가이드

이 문서는 프로젝트의 gRPC 채팅(비속어 필터링) 서비스의 성능을 평가하고, 기존 REST 방식과 비교 분석하기 위한 **K6 성능 테스트 환경 사용법**을 안내합니다.

## 📁 1. 환경 구성 요소
모든 파일은 `performance-tests` 디렉토리에 위치합니다.

| 파일명 | 설명 |
|---|---|
| `install_k6.ps1` | K6를 자동으로 설치해주는 PowerShell 스크립트입니다. |
| `install_k6.ps1` | `Chocolatey` 또는 `Winget`을 이용해 K6를 설치합니다. |
| `chat_grpc_benchmark.js` | **gRPC** 프로토콜로 비속어 필터링 서비스를 호출하여 성능을 측정합니다. |
| `chat_rest_benchmark.js` | **REST API**로 비속어 필터링 서비스를 호출하여 성능을 측정합니다. (비교용) |
| `run_tests.ps1` | 테스트를 실행하고 결과를 `results` 폴더에 자동으로 저장하는 스크립트입니다. |

<br>

## 🛠 2. 설치 및 준비

### 1단계: K6 설치
`performance-tests` 폴더에서 아래 명령어를 실행하세요. (관리자 권한 권장)
```powershell
./install_k6.ps1
```
설치가 완료되면 `k6 version`이 출력됩니다.

> [!TIP]
> **설치 후 `k6` 명령어가 인식되지 않는 경우**
> 1. 터미널(PowerShell)을 껐다가 다시 켜세요. (재시작 시 환경 변수 반영)
> 2. 또는 `run_tests.ps1` 스크립트를 사용하세요. (자동으로 기본 설치 경로를 탐색하여 실행해줍니다)

### 2단계: 서버 실행
프로젝트 백엔드 서버를 로컬에서 실행합니다.
- Spring Boot Server (기본 포트: 8080)
- gRPC Server (기본 포트: 9090)

### 3단계: 필수 보안 설정 (중요)
테스트 스크립트가 인증 토큰 없이 API를 호출할 수 있도록, `SecurityConfig.java`에 예외 설정을 추가해야 합니다.

1. `src/main/java/.../global/config/SecurityConfig.java` 파일을 엽니다.
2. `PERMIT_ALL_PATTERNS` 배열에 `/api/test/**`를 추가합니다.

```java
private static final String[] PERMIT_ALL_PATTERNS = {
    // ... 기존 경로들
    "/api/test/**" // 성능 테스트용 (임시)
};
```
> **주의**: 이 설정을 적용한 후에는 반드시 **서버를 재시작**해야 합니다.

<br>

## ⚡ 3. 성능 테스트 실행

### 간편 실행 (권장)
`run_tests.ps1` 스크립트를 사용하면 gRPC와 REST 테스트를 한 번에 실행하고 결과까지 저장할 수 있습니다.

```powershell
# gRPC와 REST 모두 실행
./run_tests.ps1

# gRPC만 실행
./run_tests.ps1 -Type grpc

# REST만 실행
./run_tests.ps1 -Type rest
```

### 수동 실행
개별적으로 옵션을 조정하여 실행하고 싶다면 아래 명령어를 사용하세요.

```powershell
# gRPC 테스트
k6 run chat_grpc_benchmark.js

# REST 테스트
k6 run chat_rest_benchmark.js
```

<br>

## 📊 4. 테스트 시나리오 및 결과 분석

### ⏳ 테스트 부하 설정 (Load Profile)
현재 스크립트(`options.stages`)에는 다음과 같은 **부하 시나리오**가 설정되어 있습니다.

| 단계 | 지속 시간 | 목표 사용자 수 (VUs) | 설명 |
|---|---|---|---|
| **Ramp-up** | 30초 | 0명 → 10명 | 사용자가 서서히 들어오는 단계 |
| **Steady** | 1분 | 10명 유지 | 10명이 동시에 계속 요청을 보내는 단계 (메인 테스트) |
| **Ramp-down** | 30초 | 10명 → 0명 | 사용자가 서서히 빠져나가는 단계 |

* **총 소요 시간**: 2분
* **동시 접속자(VUs)**: 최대 10명
* **요청 빈도**: 각 가상 사용자(VU)는 요청 후 1초(`sleep(1)`) 대기하므로, 1인당 약 1초에 1번 요청을 보냅니다. (이론상 최대 10 RPS)

> **설정을 바꾸고 싶다면?**
> `.js` 파일의 `options.stages` 부분을 수정하세요. 예를 들어 `target: 100`으로 바꾸면 100명이 동시에 접속합니다.

<br>

## 📈 결과 해석 방법

테스트가 끝나면 콘솔에 요약 정보가 출력됩니다. 다음 지표들을 중점적으로 비교하세요.

### 주요 비교 지표
| 지표 (Metric) | 설명 | gRPC 예상 | REST 예상 |
|---|---|---|---|
| **http_req_duration** | 요청 처리 소요 시간 (Latency) | **매우 낮음** (빠름) | 보통 |
| **grpc_req_duration** | gRPC 전용 처리 시간 지표 | **매우 낮음** | - |
| **iterations** | 테스트 기간 동안 처리한 총 요청 수 (Throughput) | **높음** | gRPC보다 낮음 |
| **p(95)** | 하위 95% 요청의 최대 지연 시간 (대부분의 유저가 겪는 속도) | 안정적 | 상대적으로 높을 수 있음 |

### REST 비교용 엔드포인트
비교를 위해 `ChatController`에 임시로 `/api/test/slang-check` 엔드포인트를 추가했습니다. 이 API는 내부적으로 gRPC 로직과 동일하거나 유사한 로직을 수행하되, **REST 프로토콜 오버헤드**를 포함하여 측정할 수 있도록 설계되었습니다.

> [!NOTE]
> gRPC는 HTTP/2 기반의 Binary 프로토콜을 사용하여 데이터 크기가 작고 파싱 속도가 빠르므로, 일반적으로 REST보다 **10~30% 이상의 성능 향상**을 보입니다. 이 테스트를 통해 구체적인 수치를 확인해보세요!

<br>

## ❓ 5. 트러블슈팅 (자주 발생하는 오류)

### Q1. `k6` 명령어를 찾을 수 없다고 뜹니다.
```text
k6 : 'k6' 용어가 cmdlet, 함수, 스크립트 파일... 인식되지 않습니다.
```
**해결책:**
1. 터미널을 재시작하여 환경 변수를 갱신하세요.
2. 또는 `run_tests.ps1`을 사용하세요. 이 스크립트는 설치 경로(`C:\Program Files\k6`)를 자동으로 탐색하여 실행합니다.

### Q2. 테스트 결과에서 실패율(`checks_failed`)이 100%입니다.
```text
checks_failed......: 100.00%
http_req_failed....: 100.00%
```
**해결책:**
- **보안 설정 확인**: Spring Security가 해당 API 접근을 막고 있을 수 있습니다.
- `SecurityConfig.java`에 `/api/test/**` 경로가 `permitAll()`로 설정되어 있는지 확인하세요.
- 설정을 변경했다면 **반드시 서버를 재시작**해야 합니다.

### Q3. `connect: connection refused` 오류가 발생합니다.
**해결책:**
- **REST**: `localhost:8080` 포트에 백엔드 서버가 켜져 있는지 확인하세요.
- **gRPC**: `chat_grpc_benchmark.js` 파일 내의 주소가 올바른지 확인하세요.
    - 로컬 서버라면: `localhost:9090`
    - 외부(AI) 서버라면: `xxx.ngrok-free.dev:443` (TLS 사용 여부 체크)

## 🧩 5. 크롬 익스텐션 상태 동기화 성능 테스트

### 테스트 목적
호스트와 다수의 게스트가 함께 영상을 시청할 때, 재생 상태(재생/일시정지/탐색)가 게스트들에게 얼마나 빠르게 전파되는지 **동기화 지연 시간(Latency)**을 측정합니다.

### 테스트 환경 및 조건
| 항목 | 설정 값 | 설명 |
|---|---|---|
| **프로토콜** | WebSocket (STOMP) | 양방향 실시간 통신을 사용합니다. |
| **총 사용자** | 50명 | 1명의 호스트 + 49명의 게스트 |
| **테스트 지속 시간** | 1분 | 연결 수립 및 안정화 후 1분간 측정 |
| **메시지 빈도** | 초당 1회 | 호스트가 1초마다 현재 재생 위치를 서버로 전송 |

### 시나리오 상세
1. **Setup**:
    - 호스트가 파티를 생성하고 WebSocket에 연결합니다.
    - 49명의 게스트가 해당 파티에 입장하고 WebSocket에 연결하며 구독(`SUBSCRIBE`)합니다.
2. **Action (Host)**:
    - 호스트는 1초마다 `/pub/party/{partyId}/extension/state`로 재생 상태(SEEK 등)를 전송합니다.
3. **Response (Guest)**:
    - 게스트는 `/sub/party/{partyId}/extension` 토픽을 구독하고 있습니다.
    - 서버로부터 메시지를 수신하면, 메시지 내의 원본 `timestamp`와 현재 수신 시각을 비교하여 지연 시간을 계산합니다.

### 실행 방법
```powershell
./run_tests.ps1 -Type extension
```

### 주요 측정 지표
- **sync_latency**: 호스트가 상태를 보낸 시점부터 게스트가 수신하기까지 걸린 시간 (ms)
- **message_rate**: 초당 처리되는 동기화 메시지 수

<br>

## 🔁 6. 크롬 익스텐션 상태 동기화 - 폴링(Polling) 성능 테스트

### 테스트 목적
WebSocket 방식과의 성능 비교를 위해, 전통적인 **HTTP Polling** 방식의 동기화 성능을 측정합니다.
호스트가 상태를 업데이트하고, 게스트가 주기적으로 서버에 요청을 보내 상태를 가져오는 구조입니다.

### 테스트 환경 및 조건
| 항목 | 설정 값 | 설명 |
|---|---|---|
| **프로토콜** | HTTP REST | 주기적인 요청/응답 (Request/Response) 모델 |
| **총 사용자** | 50명 | 1명의 호스트 + 49명의 게스트 |
| **테스트 지속 시간** | 1분 | |
| **Polling 간격** | 1초 | 모든 게스트가 1초마다 상태 조회 요청 (`GET`) |

### 시나리오 상세
1. **Host (POST)**: 1초마다 `/parties/{partyId}/state/polling`으로 상태 업데이트 요청
2. **Guest (GET)**: 1초마다 `/parties/{partyId}/state/polling`으로 상태 조회 요청

### 실행 방법
```powershell
./run_tests.ps1 -Type polling
```

### 주요 측정 지표
- **polling_latency**: 요청을 보내고 응답을 받기까지 걸린 시간 (RTT, Round Trip Time)
- **request_rate**: 초당 처리되는 HTTP 요청 수


