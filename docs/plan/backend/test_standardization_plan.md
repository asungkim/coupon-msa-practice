# Feature: 테스트 환경 표준화 (Docker + MySQL + k6)

## 1. Problem Definition

현재 부하 테스트의 문제점:

- **리소스 제한 없음**: 로컬 JVM이 내 맥북의 CPU/메모리를 마음대로 써서 결과가 일관되지 않음
- **H2 인메모리**: 재시작 시 초기화, 실제 운영 DB(MySQL/PostgreSQL)와 락 동작이 다름
  - 500명 원자적 UPDATE에서 이상 동작을 본 것도 H2 특성 의심
- **테스트 클라이언트 한계**: WebClient 직접 호출 방식은 TPS/p99 레이턴시 측정 불가, 셋업 시간이 측정에 섞임
- **셋업 미분리**: 유저/쿠폰 생성 시간 + 동시 발급 시간이 하나로 묶여서 "진짜 발급 성능"을 모름

→ 정확한 수치 없이는 "어떤 전략이 더 좋다"를 말할 수 없음

## 2. Prerequisites

- ✅ Phase 1, 2, 2.5 완료
- Docker 설치 (로컬)
- k6 설치 (`brew install k6`)

## 3. 결정된 선택지

| 항목 | 결정 | 근거 |
|------|------|------|
| 리소스 격리 | **Docker Compose** | CPU/메모리 제한으로 일관된 환경 보장, 운영과 유사 |
| DB | **MySQL 8.x (Docker)** | 실무 표준. 선착순 발급에서 원자적 UPDATE 성능 우수. PostgreSQL 대비 생태계 풍부 |
| 부하 테스트 도구 | **k6** | JavaScript 시나리오, CLI 친화적, 실시간 메트릭, CI 연동 쉬움 |
| 셋업 분리 | **Pre-seed 스크립트** + **k6 setup 단계** | 유저/쿠폰 생성을 측정에서 분리 |

## 4. 아키텍처

```
┌──────────────────── Docker Compose ────────────────────┐
│                                                          │
│  mysql (3306)    ◄─────────┐                            │
│                              │                            │
│  coupon-api-1 (8080)  ──────┤                            │
│  coupon-api-2 (8081)  ──────┤                            │
│    (cpus=2, memory=1g each) │                            │
│                              │                            │
│  notification-mock (8090)   │                            │
│  gateway (9000)             │                            │
│                                                          │
└──────────────────────────────────────────────────────────┘
         ▲
         │ k6 부하 테스트 (로컬에서 실행)
         │
    k6 스크립트 (JavaScript)
    - setup(): 쿠폰만 생성 (유저는 pre-seed)
    - default fn: 동시 발급 요청
    - teardown(): 결과 DB 검증
```

## 5. 구현 계획

### 5.1 MySQL 전환

**의존성 변경 (`coupon-api/build.gradle`):**
```groovy
runtimeOnly 'com.mysql:mysql-connector-j'
// H2는 테스트용으로만 유지
```

**application.yml:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/coupon?useSSL=false&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: coupon
    password: coupon
    hikari:
      maximum-pool-size: 10
  jpa:
    hibernate:
      ddl-auto: create-drop  # Docker 환경에서도 매번 초기화
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
```

**테스트 프로파일은 H2 유지** — 단위/통합 테스트는 빠른 H2로 계속.

### 5.2 Docker Compose 구성

**리소스 제한 기준 (AWS EC2 스펙 기준):**

| 서비스 | EC2 스펙 | vCPU | RAM | 근거 |
|--------|---------|------|-----|------|
| coupon-api-1 | t2.small | 1 | 2g | 실제 운영 시 일반적인 API 서버 최소 스펙 |
| coupon-api-2 | t2.small | 1 | 2g | 동일 |
| mysql | t2.small | 1 | 2g | API 서버와 동일 스펙 — DB 병목도 관찰 |
| gateway | t2.small | 1 | 2g | 요청 라우팅만 담당하지만 WebFlux JVM이라 t2.small |
| notification-mock | t2.micro | 1 | 1g | mock 서버라 가벼움 |

```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8.4
    environment:
      MYSQL_DATABASE: coupon
      MYSQL_USER: coupon
      MYSQL_PASSWORD: coupon
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    deploy:
      resources:
        limits:
          cpus: '1'        # t2.small
          memory: 2g

  notification-mock:
    build: ./notification-mock-api
    ports:
      - "8090:8090"
    deploy:
      resources:
        limits:
          cpus: '1'        # t2.micro
          memory: 1g

  coupon-api-1:
    build: ./coupon-api
    environment:
      SERVER_PORT: 8080
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/coupon
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - notification-mock
    deploy:
      resources:
        limits:
          cpus: '1'        # t2.small
          memory: 2g

  coupon-api-2:
    build: ./coupon-api
    environment:
      SERVER_PORT: 8081
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/coupon
    ports:
      - "8081:8081"
    depends_on:
      - mysql
      - notification-mock
    deploy:
      resources:
        limits:
          cpus: '1'        # t2.small
          memory: 2g

  gateway:
    build: ./gateway
    ports:
      - "9000:9000"
    depends_on:
      - coupon-api-1
      - coupon-api-2
    deploy:
      resources:
        limits:
          cpus: '1'        # t2.small
          memory: 2g
```

각 모듈에 `Dockerfile` 추가 필요.

### 5.3 k6 스크립트

**k6/load-test.js 구조:**

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        burst: {
            executor: 'per-vu-iterations',
            vus: __ENV.VUS || 300,
            iterations: 1,
            maxDuration: '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<3000'],
        http_req_failed: ['rate<0.5'],
    },
};

// 셋업: 쿠폰 하나 생성 (유저는 pre-seed로 미리 존재)
export function setup() {
    const runId = Date.now();
    const res = http.post(`${__ENV.BASE_URL}/api/coupons`, JSON.stringify({
        name: `k6쿠폰_${runId}`,
        description: 'k6 부하 테스트',
        pointCost: 100,
        totalQuantity: __ENV.QUANTITY || 200,
        startDate: '2026-01-01T00:00:00',
        endDate: '2027-01-01T00:00:00',
    }), { headers: { 'Content-Type': 'application/json' } });
    return { couponId: res.json('id') };
}

// 각 VU가 한 번씩 발급 요청
export default function (data) {
    const userId = __VU;  // VU 번호를 userId로 매핑 (pre-seed된 유저)
    const res = http.post(
        `${__ENV.BASE_URL}/api/coupons/${data.couponId}/issue`,
        JSON.stringify({ userId }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, {
        'status is 2xx or 4xx': (r) => r.status < 500,
    });
}

// 결과 검증: DB 상태 조회
export function teardown(data) {
    const coupon = http.get(`${__ENV.BASE_URL}/api/coupons/${data.couponId}`).json();
    const issueCount = http.get(`${__ENV.BASE_URL}/api/coupons/${data.couponId}/issues/count`).body;
    console.log(`=== DB 검증 ===`);
    console.log(`remainingQuantity: ${coupon.remainingQuantity}`);
    console.log(`issueCount: ${issueCount}`);
    console.log(`consumed: ${coupon.totalQuantity - coupon.remainingQuantity}`);
    console.log(`정합성: ${coupon.totalQuantity - coupon.remainingQuantity == issueCount ? 'PASS' : 'FAIL'}`);
}
```

### 5.4 Pre-seed 스크립트

유저 N명을 사전에 생성하는 스크립트 — 매 테스트 전에 실행 (또는 `data.sql`로 자동 로드):

```javascript
// k6/seed-users.js
import http from 'k6/http';

export const options = {
    vus: 10,
    iterations: __ENV.USERS || 1000,
};

export default function () {
    const i = __ITER + 1;
    const res = http.post(`${__ENV.BASE_URL}/api/users`, JSON.stringify({
        email: `seed${i}@test.com`,
        name: `SeedUser${i}`,
    }), { headers: { 'Content-Type': 'application/json' } });
    const userId = res.json('id');
    http.post(`${__ENV.BASE_URL}/api/users/${userId}/points`,
        JSON.stringify({ amount: 10000 }),
        { headers: { 'Content-Type': 'application/json' } });
}
```

### 5.5 실행 플로우

```bash
# 1. Docker Compose 기동
docker-compose up -d

# 2. 유저 pre-seed
BASE_URL=http://localhost:8080 USERS=1000 k6 run k6/seed-users.js

# 3. 부하 테스트 (단일 서버)
BASE_URL=http://localhost:8080 VUS=1000 QUANTITY=200 k6 run k6/load-test.js

# 4. 부하 테스트 (Gateway 분산)
BASE_URL=http://localhost:9000 VUS=1000 QUANTITY=200 k6 run k6/load-test.js

# 5. 결과 기록 (k6 JSON 출력 + docs/history/ 저장)
k6 run --out json=results.json k6/load-test.js
```

## 6. 패키지 구조 (추가분)

```
coupon-msa/
├── docker-compose.yml                (신규)
├── coupon-api/
│   └── Dockerfile                    (신규)
├── notification-mock-api/
│   └── Dockerfile                    (신규)
├── gateway/
│   └── Dockerfile                    (신규)
├── k6/                               (신규 디렉토리)
│   ├── load-test.js
│   └── seed-users.js
└── docs/history/
    └── load_test_results.md          (기존 — k6 결과 추가)
```

## 7. TDD Plan

### Task 1: MySQL 전환

1. `coupon-api/build.gradle`에 MySQL 드라이버 추가
2. `application.yml` — MySQL 기본 + 테스트 프로파일은 H2
3. 엔티티에 MySQL 호환 설정 (VARCHAR 길이 등)
4. 로컬 MySQL(Docker) 띄워서 애플리케이션 구동 확인

### Task 2: Dockerfile + docker-compose

1. 각 모듈에 Dockerfile 작성 (Spring Boot 표준)
2. docker-compose.yml 작성 (MySQL + 3개 앱 + 리소스 제한)
3. `docker-compose up` → 모든 서비스 기동 확인

### Task 3: k6 스크립트

1. `k6/seed-users.js` — 유저 N명 pre-seed
2. `k6/load-test.js` — 부하 시나리오 + DB 정합성 검증
3. 실행 스크립트 문서화 (`docs/` 또는 `README`)

### Task 4: 기준 벤치마크 측정

1. Docker Compose 기동 → pre-seed → 부하 테스트 실행
2. 300명, 1000명 각 3회씩 측정
3. TPS, p95/p99 레이턴시, 에러율, DB 정합성 기록
4. 기존 수치와 비교 (Docker 격리 전/후)

## 8. Verification Criteria

- [ ] `docker-compose up`으로 모든 서비스가 리소스 제한 하에 기동됨
- [ ] MySQL 전환 후 기존 단위/통합 테스트 전체 통과 (테스트는 H2 유지)
- [ ] k6 스크립트가 TPS, p95/p99, 에러율, DB 정합성을 출력
- [ ] 동일 조건으로 여러 번 실행해도 결과가 일관됨 (±10% 이내)
- [ ] 결과가 `docs/history/load_test_results.md`에 정리됨

## 9. Design Decisions

| 결정 | 근거 |
|------|------|
| Docker Compose로 환경 고정 | 로컬 변동성 제거. 운영 환경 유사 |
| 모든 서버 t2.small (1vCPU/2g) | 동일 스펙에서 병목이 어디인지(API vs DB) 관찰. 제약된 환경에서의 성능 측정이 학습 목표 |
| MySQL 8.x 선택 | 실무 표준, 선착순 발급에 적합, PostgreSQL 대비 생태계 |
| 단위 테스트는 H2 유지 | 빠른 피드백 루프 보존. 통합/부하만 MySQL |
| k6 선택 | 경량, JS, CLI, 실시간 메트릭 |
| 유저 pre-seed 분리 | 셋업 시간을 측정에서 제외. 발급 TPS만 순수 측정 |
| `per-vu-iterations` executor | 각 VU가 한 번씩 발급 (한 유저 1쿠폰 원칙 유지) |
| VU 번호를 userId로 매핑 | pre-seed된 유저와 매칭. 유저 조회 불필요 |
