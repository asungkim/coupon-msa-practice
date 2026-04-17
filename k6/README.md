# k6 부하 테스트

## 전제

Docker Compose로 서비스가 기동되어 있어야 한다:

```bash
docker compose up -d --build
```

## 1. 유저 pre-seed

부하 테스트 전에 유저를 미리 생성해 둔다. DB가 초기화되면 다시 실행해야 한다.

```bash
BASE_URL=http://localhost:8080 USERS=1000 k6 run k6/seed-users.js
```

## 2. 부하 테스트 실행

### 단일 인스턴스 (8080)

```bash
BASE_URL=http://localhost:8080 VUS=1000 QUANTITY=200 k6 run k6/load-test.js
```

### Gateway 경유 2대 분산 (9000)

```bash
BASE_URL=http://localhost:9000 VUS=1000 QUANTITY=200 k6 run k6/load-test.js
```

## 출력 예시

k6는 다음 메트릭을 자동으로 출력한다:
- `http_req_duration`: p50, p90, p95, p99 레이턴시
- `http_reqs`: 총 요청 수, TPS
- `http_req_failed`: 실패율
- 그 외 VU, 데이터 전송량 등

`teardown` 단계에서 DB 정합성도 검증한다.

## 전략 전환

쿠폰 서버의 전략을 바꾸려면:
1. `coupon-api/src/main/resources/application.yml`의 `coupon.issue.strategy` 변경 (`pessimistic` | `atomic`)
2. `docker compose up -d --build coupon-api-1 coupon-api-2`로 재빌드

## 실시간 메트릭 관찰

별도 터미널에서:

```bash
./k6/live-metrics.sh
# 또는 특정 서버:
./k6/live-metrics.sh http://localhost:8081 1
```

표시되는 지표:
- **CPU**: process / system (80% 넘으면 빨강)
- **JVM 힙**: 사용 / 최대
- **JVM threads**: 활성 스레드 수
- **HikariCP**: active / idle / pending / timeout
  - `pending > 10`이면 노랑, `> 50`이면 빨강 (커넥션 대기 큐)
  - `timeout > 0`이면 빨강 (커넥션 타임아웃 발생)

부하 테스트 실행 중에 옆 터미널에서 띄워두면 **서버가 어떻게 터지는지 실시간 관찰** 가능.
