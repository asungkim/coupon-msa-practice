# 부하 테스트 결과 기록

> 각 단계별 동일 조건(300명/200쿠폰)으로 실행한 결과를 누적 기록한다.

## [2026-04-15 18:01] Phase 1 — 트랜잭션 내 알림 동기호출 + Java 레벨 재고차감

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 300 |
| HTTP 200 성공 | 31 |
| HTTP 4xx 실패 | 0 |
| 에러/타임아웃 | 269 |
| 총 소요시간 | 10.0초 |
| remainingQuantity | 179 |
| 실제 발급 건수(DB) | 213 |
| 소비량(total-remaining) | 21 |
| 정합성(소비량==발급건수) | FAIL |

## [2026-04-15 18:08] Step 1 — 이벤트 분리 + 동기 호출 (RestClient)

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 300 |
| HTTP 200 성공 | 33 |
| HTTP 4xx 실패 | 0 |
| 에러/타임아웃 | 267 |
| 총 소요시간 | 10.0초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 217 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | FAIL |

## [2026-04-15 18:11] Step 2 — 이벤트 분리 + 비동기 호출 (WebClient subscribe)

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 300 |
| HTTP 200 성공 | 300 |
| HTTP 4xx 실패 | 0 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 0.5초 |
| remainingQuantity | 147 |
| 실제 발급 건수(DB) | 300 |
| 소비량(total-remaining) | 53 |
| 정합성(소비량==발급건수) | FAIL |

## [2026-04-15 19:08] 전략 A — 비관적 락 (SELECT FOR UPDATE)

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 300 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 100 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 0.8초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 19:09] 전략 B — 원자적 UPDATE (UPDATE WHERE qty > 0)

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 300 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 100 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 0.6초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 19:14] 전략 A — 비관적 락 (500명/200쿠폰)

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 500 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 300 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 0.9초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 19:16] 전략 B — 원자적 UPDATE (500명/200쿠폰)

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 500 |
| HTTP 200 성공 | 177 |
| HTTP 4xx 실패 | 0 |
| 에러/타임아웃 | 323 |
| 총 소요시간 | 16.0초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 19:23] 전략 B — 원자적 UPDATE (500명/200쿠폰) 재실행

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 500 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 300 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 0.9초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 19:27] 전략 A — 비관적 락 (1000명/200쿠폰)

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 1000 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 800 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 1.1초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 19:28] 전략 B — 원자적 UPDATE (1000명/200쿠폰)

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 1000 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 800 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 0.9초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 22:07] Gateway 분산 — 원자적 UPDATE (300명/200쿠폰, 2대)

| 항목 | 값 |
|------|-----|
| 서버 구성 | Gateway(9000) → 8080 + 8081 (2대 분산) |
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 300 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 100 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 1.5초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 22:08] Gateway 분산 — 원자적 UPDATE (1000명/200쿠폰, 2대)

| 항목 | 값 |
|------|-----|
| 서버 구성 | Gateway(9000) → 8080 + 8081 (2대 분산) |
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 1000 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 800 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 30.5초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 22:13] 단일(8080) — 원자적 UPDATE (1000명/200쿠폰) #1

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 1000 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 800 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 1.0초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 22:14] 단일(8080) — 원자적 UPDATE (1000명/200쿠폰) #2

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 1000 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 800 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 0.6초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 22:14] 단일(8080) — 원자적 UPDATE (1000명/200쿠폰) #3

| 항목 | 값 |
|------|-----|
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 1000 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 800 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 0.6초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 22:15] Gateway 분산(2대) — 원자적 UPDATE (1000명/200쿠폰) #1

| 항목 | 값 |
|------|-----|
| 서버 구성 | Gateway(9000) → 8080 + 8081 (2대 분산) |
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 1000 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 800 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 29.3초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 22:16] Gateway 분산(2대) — 원자적 UPDATE (1000명/200쿠폰) #2

| 항목 | 값 |
|------|-----|
| 서버 구성 | Gateway(9000) → 8080 + 8081 (2대 분산) |
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 1000 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 800 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 31.0초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

## [2026-04-15 22:17] Gateway 분산(2대) — 원자적 UPDATE (1000명/200쿠폰) #3

| 항목 | 값 |
|------|-----|
| 서버 구성 | Gateway(9000) → 8080 + 8081 (2대 분산) |
| 쿠폰 수량 | 200 |
| 동시 요청 수 | 1000 |
| HTTP 200 성공 | 200 |
| HTTP 4xx 실패 | 800 |
| 에러/타임아웃 | 0 |
| 총 소요시간 | 31.0초 |
| remainingQuantity | 0 |
| 실제 발급 건수(DB) | 200 |
| 소비량(total-remaining) | 200 |
| 정합성(소비량==발급건수) | PASS |

---

# Phase 3 기준 벤치마크 (Docker + MySQL + k6)

> 환경: Docker Compose로 각 서버 리소스 제한 (t2.small: 1vCPU/2g).
> DB: MySQL 8.4 (t2.small).
> 도구: k6 (per-vu-iterations executor, VU 1000 = 유저 1000명).
> 전략: atomic (원자적 UPDATE).
> 알림: async (WebClient subscribe).
> 유저는 pre-seed로 미리 생성(측정 제외).

## [2026-04-16 16:31] 단일 인스턴스 (8080 직접) — k6 첫 측정

| 항목 | 값 |
|------|-----|
| 서버 구성 | coupon-api-1 단일 (t2.small) |
| 쿠폰 수량 | 200 |
| 동시 VU 수 | 1000 |
| HTTP 요청 수 | 1003 |
| HTTP 4xx (재고 소진 포함) | 800 (79.76%) |
| TPS | 약 100 req/s |
| avg latency | 6.98초 |
| p95 latency | 9.42초 |
| p99 latency | 9.54초 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 정합성 | PASS ✅ |

## [2026-04-16 16:32] Gateway 분산 (2대) — k6 첫 측정

| 항목 | 값 |
|------|-----|
| 서버 구성 | Gateway → coupon-api-1 + coupon-api-2 (각 t2.small) |
| 쿠폰 수량 | 200 |
| 동시 VU 수 | 1000 |
| HTTP 요청 수 | 1003 |
| HTTP 4xx | 800 (79.76%) |
| TPS | 약 140 req/s |
| avg latency | 5.19초 |
| p95 latency | 6.61초 |
| p99 latency | 6.71초 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 정합성 | PASS ✅ |

## [2026-04-16 16:33] Gateway 분산 (2대) — k6 두번째 측정

| 항목 | 값 |
|------|-----|
| 서버 구성 | Gateway → coupon-api-1 + coupon-api-2 |
| 쿠폰 수량 | 200 |
| 동시 VU 수 | 1000 |
| avg latency | 2.7초 |
| p95 latency | 3.65초 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 정합성 | PASS ✅ |

## 분석

- **정합성**: 3회 모두 정확히 200건 발급, remainingQuantity 0 — 원자적 UPDATE 전략이 모든 조건에서 안정적
- **Gateway 분산이 오히려 빠름** (단일 100 TPS vs 분산 140 TPS, latency도 낮음)
  - 이전 로컬 WebClient 테스트와 반대 결과
  - 이유 추정: MySQL 전환 후 DB가 실제로 병목이 되면서 API 서버 CPU가 여유 있게 되고, 2대로 분산했을 때 각 서버의 부하가 분산돼 latency 개선
  - H2 인메모리 때는 DB가 너무 빨라서 Gateway hop 오버헤드만 부각됐었음
- **반복성**: 2번째 측정이 더 빠른 건 JIT 최적화/커넥션 풀 워밍업 효과로 추정

## [2026-04-16 07:43] Gateway — atomic + async (300 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 300 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 300 |
| **TPS (req/s)** | 77.60 |
| iterations | 300 |
| iteration_duration avg | 2849.1 ms |
| **http_req_duration avg** | 2830.5 ms |
| http_req_duration min | 1156.5 ms |
| http_req_duration p50 | 3046.6 ms |
| http_req_duration p90 | 3685.4 ms |
| **http_req_duration p95** | 3707.7 ms |
| **http_req_duration p99** | - |
| http_req_duration max | 3807.7 ms |
| http_req_waiting avg (TTFB) | 2828.7 ms |
| **http_req_failed rate** | 33.33% |
| 200 성공 | 200 |
| 200 실패 | 100 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 62.5 KB |
| data_sent | 45.6 KB |

## [2026-04-16 07:43] Gateway — atomic + async (500 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 500 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 500 |
| **TPS (req/s)** | 85.73 |
| iterations | 500 |
| iteration_duration avg | 4449.7 ms |
| **http_req_duration avg** | 4436.2 ms |
| http_req_duration min | 1096.3 ms |
| http_req_duration p50 | 4873.9 ms |
| http_req_duration p90 | 5636.6 ms |
| **http_req_duration p95** | 5716.8 ms |
| **http_req_duration p99** | - |
| http_req_duration max | 5797.0 ms |
| http_req_waiting avg (TTFB) | 4428.7 ms |
| **http_req_failed rate** | 60.00% |
| 200 성공 | 200 |
| 200 실패 | 300 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 101.0 KB |
| data_sent | 76.1 KB |

## [2026-04-16 07:44] Gateway — atomic + async (1000 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 1000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 1000 |
| **TPS (req/s)** | 168.34 |
| iterations | 1000 |
| iteration_duration avg | 4273.0 ms |
| **http_req_duration avg** | 4230.4 ms |
| http_req_duration min | 1086.0 ms |
| http_req_duration p50 | 4461.3 ms |
| http_req_duration p90 | 5545.2 ms |
| **http_req_duration p95** | 5741.4 ms |
| **http_req_duration p99** | - |
| http_req_duration max | 5875.4 ms |
| http_req_waiting avg (TTFB) | 4227.1 ms |
| **http_req_failed rate** | 80.00% |
| 200 성공 | 200 |
| 200 실패 | 800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 197.2 KB |
| data_sent | 152.2 KB |

## [2026-04-16 07:46] Gateway — pessimistic + async (300 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 300 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 300 |
| **TPS (req/s)** | 88.90 |
| iterations | 300 |
| iteration_duration avg | 2661.1 ms |
| **http_req_duration avg** | 2651.2 ms |
| http_req_duration min | 1248.5 ms |
| http_req_duration p50 | 2830.7 ms |
| http_req_duration p90 | 3323.2 ms |
| **http_req_duration p95** | 3339.5 ms |
| **http_req_duration p99** | - |
| http_req_duration max | 3355.1 ms |
| http_req_waiting avg (TTFB) | 2648.2 ms |
| **http_req_failed rate** | 33.33% |
| 200 성공 | 200 |
| 200 실패 | 100 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 62.8 KB |
| data_sent | 45.6 KB |

## [2026-04-16 07:46] Gateway — pessimistic + async (500 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 500 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 500 |
| **TPS (req/s)** | 142.12 |
| iterations | 500 |
| iteration_duration avg | 2759.0 ms |
| **http_req_duration avg** | 2747.0 ms |
| http_req_duration min | 1027.0 ms |
| http_req_duration p50 | 2964.2 ms |
| http_req_duration p90 | 3383.2 ms |
| **http_req_duration p95** | 3459.9 ms |
| **http_req_duration p99** | - |
| http_req_duration max | 3499.9 ms |
| http_req_waiting avg (TTFB) | 2744.1 ms |
| **http_req_failed rate** | 60.00% |
| 200 성공 | 200 |
| 200 실패 | 300 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 101.3 KB |
| data_sent | 76.1 KB |

## [2026-04-16 07:47] Gateway — pessimistic + async (1000 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 1000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 1000 |
| **TPS (req/s)** | 171.55 |
| iterations | 1000 |
| iteration_duration avg | 4084.7 ms |
| **http_req_duration avg** | 4024.4 ms |
| http_req_duration min | 1339.5 ms |
| http_req_duration p50 | 4245.1 ms |
| http_req_duration p90 | 5517.5 ms |
| **http_req_duration p95** | 5605.6 ms |
| **http_req_duration p99** | - |
| http_req_duration max | 5716.3 ms |
| http_req_waiting avg (TTFB) | 4022.0 ms |
| **http_req_failed rate** | 80.00% |
| 200 성공 | 200 |
| 200 실패 | 800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 197.5 KB |
| data_sent | 152.2 KB |

## [2026-04-16 07:50] Gateway — atomic + sync (300 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 300 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 300 |
| **TPS (req/s)** | 9.75 |
| iterations | 300 |
| iteration_duration avg | 22059.3 ms |
| **http_req_duration avg** | 22040.6 ms |
| http_req_duration min | 6930.7 ms |
| http_req_duration p50 | 24781.2 ms |
| http_req_duration p90 | 29490.0 ms |
| **http_req_duration p95** | 29581.8 ms |
| **http_req_duration p99** | - |
| http_req_duration max | 30777.2 ms |
| http_req_waiting avg (TTFB) | 22039.9 ms |
| **http_req_failed rate** | 33.33% |
| 200 성공 | 200 |
| 200 실패 | 100 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 62.8 KB |
| data_sent | 45.6 KB |

## [2026-04-16 07:51] Gateway — atomic + sync (500 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 500 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 500 |
| **TPS (req/s)** | 16.96 |
| iterations | 500 |
| iteration_duration avg | 22772.7 ms |
| **http_req_duration avg** | 22756.4 ms |
| http_req_duration min | 3750.8 ms |
| http_req_duration p50 | 26949.0 ms |
| http_req_duration p90 | 27355.1 ms |
| **http_req_duration p95** | 27443.8 ms |
| **http_req_duration p99** | - |
| http_req_duration max | 29431.8 ms |
| http_req_waiting avg (TTFB) | 22753.4 ms |
| **http_req_failed rate** | 60.00% |
| 200 성공 | 200 |
| 200 실패 | 300 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 101.3 KB |
| data_sent | 76.1 KB |

## [2026-04-16 07:55] Gateway — atomic + async (300 VU) [p99 포함]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 300 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 300 |
| **TPS (req/s)** | 10.38 |
| iterations | 300 |
| iteration_duration avg | 19991.2 ms |
| **http_req_duration avg** | 19977.6 ms |
| http_req_duration min | 4621.9 ms |
| http_req_duration p50 | 22821.1 ms |
| http_req_duration p90 | 27296.1 ms |
| **http_req_duration p95** | 27333.3 ms |
| **http_req_duration p99** | 28842.5 ms |
| http_req_duration max | 28878.8 ms |
| http_req_waiting avg (TTFB) | 19972.2 ms |
| **http_req_failed rate** | 33.33% |
| 200 성공 | 200 |
| 200 실패 | 100 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 62.8 KB |
| data_sent | 45.6 KB |

## [2026-04-16 07:56] Gateway — atomic + async (500 VU) [p99 포함]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 500 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 500 |
| **TPS (req/s)** | 17.55 |
| iterations | 500 |
| iteration_duration avg | 22057.1 ms |
| **http_req_duration avg** | 22026.4 ms |
| http_req_duration min | 3852.4 ms |
| http_req_duration p50 | 25856.1 ms |
| http_req_duration p90 | 26827.6 ms |
| **http_req_duration p95** | 26934.7 ms |
| **http_req_duration p99** | 27485.5 ms |
| http_req_duration max | 28421.3 ms |
| http_req_waiting avg (TTFB) | 22002.7 ms |
| **http_req_failed rate** | 60.00% |
| 200 성공 | 200 |
| 200 실패 | 300 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 101.3 KB |
| data_sent | 76.1 KB |

## [2026-04-16 07:57] Gateway — atomic + async (1000 VU) [p99 포함]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 1000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 1000 |
| **TPS (req/s)** | 35.11 |
| iterations | 1000 |
| iteration_duration avg | 25063.4 ms |
| **http_req_duration avg** | 25038.0 ms |
| http_req_duration min | 3926.6 ms |
| http_req_duration p50 | 27246.2 ms |
| http_req_duration p90 | 28075.5 ms |
| **http_req_duration p95** | 28261.6 ms |
| **http_req_duration p99** | 28371.4 ms |
| http_req_duration max | 28441.4 ms |
| http_req_waiting avg (TTFB) | 25033.6 ms |
| **http_req_failed rate** | 80.00% |
| 200 성공 | 200 |
| 200 실패 | 800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 197.5 KB |
| data_sent | 152.2 KB |

## [2026-04-16 08:00] Gateway — atomic + async (300 VU) [재측정]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 300 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 300 |
| **TPS (req/s)** | 107.49 |
| iterations | 300 |
| iteration_duration avg | 2173.5 ms |
| **http_req_duration avg** | 2151.9 ms |
| http_req_duration min | 956.1 ms |
| http_req_duration p50 | 2288.0 ms |
| http_req_duration p90 | 2708.0 ms |
| **http_req_duration p95** | 2724.4 ms |
| **http_req_duration p99** | 2753.0 ms |
| http_req_duration max | 2755.1 ms |
| http_req_waiting avg (TTFB) | 2150.0 ms |
| **http_req_failed rate** | 33.33% |
| 200 성공 | 200 |
| 200 실패 | 100 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 62.5 KB |
| data_sent | 45.6 KB |

## [2026-04-16 08:01] Gateway — atomic + async (500 VU) [재측정]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 500 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 500 |
| **TPS (req/s)** | 120.47 |
| iterations | 500 |
| iteration_duration avg | 3213.0 ms |
| **http_req_duration avg** | 3184.0 ms |
| http_req_duration min | 1319.7 ms |
| http_req_duration p50 | 3419.7 ms |
| http_req_duration p90 | 3884.9 ms |
| **http_req_duration p95** | 3911.0 ms |
| **http_req_duration p99** | 4077.9 ms |
| http_req_duration max | 4102.4 ms |
| http_req_waiting avg (TTFB) | 3181.9 ms |
| **http_req_failed rate** | 60.00% |
| 200 성공 | 200 |
| 200 실패 | 300 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 101.0 KB |
| data_sent | 76.1 KB |

## [2026-04-16 08:01] Gateway — atomic + async (1000 VU) [재측정]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 1000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 1000 |
| **TPS (req/s)** | 94.59 |
| iterations | 1000 |
| iteration_duration avg | 6665.2 ms |
| **http_req_duration avg** | 6631.4 ms |
| http_req_duration min | 1590.8 ms |
| http_req_duration p50 | 6622.6 ms |
| http_req_duration p90 | 9898.7 ms |
| **http_req_duration p95** | 10216.0 ms |
| **http_req_duration p99** | 10410.1 ms |
| http_req_duration max | 10429.6 ms |
| http_req_waiting avg (TTFB) | 6603.1 ms |
| **http_req_failed rate** | 80.00% |
| 200 성공 | 200 |
| 200 실패 | 800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 199.7 KB |
| data_sent | 152.2 KB |

## [2026-04-16 08:03] Gateway — pessimistic + async (300 VU) [재측정]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 300 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 300 |
| **TPS (req/s)** | 88.02 |
| iterations | 300 |
| iteration_duration avg | 2528.8 ms |
| **http_req_duration avg** | 2512.4 ms |
| http_req_duration min | 1122.9 ms |
| http_req_duration p50 | 2639.5 ms |
| http_req_duration p90 | 3238.7 ms |
| **http_req_duration p95** | 3258.6 ms |
| **http_req_duration p99** | 3351.2 ms |
| http_req_duration max | 3358.6 ms |
| http_req_waiting avg (TTFB) | 2507.3 ms |
| **http_req_failed rate** | 33.33% |
| 200 성공 | 200 |
| 200 실패 | 100 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 62.8 KB |
| data_sent | 45.6 KB |

## [2026-04-16 08:03] Gateway — pessimistic + async (500 VU) [재측정]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 500 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 500 |
| **TPS (req/s)** | 86.67 |
| iterations | 500 |
| iteration_duration avg | 4177.0 ms |
| **http_req_duration avg** | 4130.9 ms |
| http_req_duration min | 1760.4 ms |
| http_req_duration p50 | 4338.3 ms |
| http_req_duration p90 | 5392.4 ms |
| **http_req_duration p95** | 5560.0 ms |
| **http_req_duration p99** | 5604.8 ms |
| http_req_duration max | 5697.5 ms |
| http_req_waiting avg (TTFB) | 4128.2 ms |
| **http_req_failed rate** | 60.00% |
| 200 성공 | 200 |
| 200 실패 | 300 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 101.3 KB |
| data_sent | 76.1 KB |

## [2026-04-16 08:04] Gateway — pessimistic + async (1000 VU) [재측정]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 1000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 1000 |
| **TPS (req/s)** | 109.71 |
| iterations | 1000 |
| iteration_duration avg | 6695.7 ms |
| **http_req_duration avg** | 6659.2 ms |
| http_req_duration min | 1360.2 ms |
| http_req_duration p50 | 7167.0 ms |
| http_req_duration p90 | 8781.6 ms |
| **http_req_duration p95** | 8956.1 ms |
| **http_req_duration p99** | 9028.1 ms |
| http_req_duration max | 9046.2 ms |
| http_req_waiting avg (TTFB) | 6650.1 ms |
| **http_req_failed rate** | 80.00% |
| 200 성공 | 200 |
| 200 실패 | 800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 197.5 KB |
| data_sent | 152.2 KB |

## [2026-04-16 08:07] Gateway — atomic + sync (300 VU) [재측정]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 300 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 300 |
| **TPS (req/s)** | 23.57 |
| iterations | 300 |
| iteration_duration avg | 3275.5 ms |
| **http_req_duration avg** | 3265.5 ms |
| http_req_duration min | 9.7 ms |
| http_req_duration p50 | 516.9 ms |
| http_req_duration p90 | 9854.2 ms |
| **http_req_duration p95** | 10159.2 ms |
| **http_req_duration p99** | 10634.5 ms |
| http_req_duration max | 12707.5 ms |
| http_req_waiting avg (TTFB) | 3264.4 ms |
| **http_req_failed rate** | 89.00% |
| 200 성공 | 33 |
| 200 실패 | 267 |
| totalQuantity | undefined |
| remainingQuantity | undefined |
| 실제 발급(DB) | 33 |
| 소비량(total-remaining) | NaN |
| **정합성** | FAIL ❌ |
| data_received | 62.5 KB |
| data_sent | 45.6 KB |

## [2026-04-16 08:08] Gateway — atomic + sync (500 VU) [재측정]

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 500 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 500 |
| **TPS (req/s)** | 26.51 |
| iterations | 500 |
| iteration_duration avg | 5582.6 ms |
| **http_req_duration avg** | 5566.6 ms |
| http_req_duration min | 9.2 ms |
| http_req_duration p50 | 363.4 ms |
| http_req_duration p90 | 16377.9 ms |
| **http_req_duration p95** | 16424.0 ms |
| **http_req_duration p99** | 17530.5 ms |
| http_req_duration max | 18790.4 ms |
| http_req_waiting avg (TTFB) | 5560.0 ms |
| **http_req_failed rate** | 88.00% |
| 200 성공 | 60 |
| 200 실패 | 440 |
| totalQuantity | undefined |
| remainingQuantity | undefined |
| 실제 발급(DB) | 60 |
| 소비량(total-remaining) | NaN |
| **정합성** | FAIL ❌ |
| data_received | 104.5 KB |
| data_sent | 76.1 KB |

## [2026-04-16 08:18] Gateway — atomic + async (2000 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 2000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 2000 |
| **TPS (req/s)** | 201.60 |
| iterations | 2000 |
| iteration_duration avg | 7641.4 ms |
| **http_req_duration avg** | 7513.3 ms |
| http_req_duration min | 2616.5 ms |
| http_req_duration p50 | 7858.2 ms |
| http_req_duration p90 | 9081.9 ms |
| **http_req_duration p95** | 9287.9 ms |
| **http_req_duration p99** | 9492.1 ms |
| http_req_duration max | 9592.0 ms |
| http_req_waiting avg (TTFB) | 7503.1 ms |
| **http_req_failed rate** | 90.00% |
| 200 성공 | 200 |
| 200 실패 | 1800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 389.6 KB |
| data_sent | 305.6 KB |

## [2026-04-16 08:20] Gateway — pessimistic + async (2000 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 2000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 2000 |
| **TPS (req/s)** | 160.93 |
| iterations | 2000 |
| iteration_duration avg | 8411.0 ms |
| **http_req_duration avg** | 8361.6 ms |
| http_req_duration min | 1322.9 ms |
| http_req_duration p50 | 9004.5 ms |
| http_req_duration p90 | 11854.9 ms |
| **http_req_duration p95** | 12055.0 ms |
| **http_req_duration p99** | 12177.9 ms |
| http_req_duration max | 12275.9 ms |
| http_req_waiting avg (TTFB) | 8349.0 ms |
| **http_req_failed rate** | 90.00% |
| 200 성공 | 200 |
| 200 실패 | 1800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 392.0 KB |
| data_sent | 305.6 KB |

## [2026-04-16 08:24] Gateway — atomic + async (2000 VU) #2

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 2000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 2000 |
| **TPS (req/s)** | 247.30 |
| iterations | 2000 |
| iteration_duration avg | 5765.8 ms |
| **http_req_duration avg** | 5695.3 ms |
| http_req_duration min | 1432.2 ms |
| http_req_duration p50 | 6108.8 ms |
| http_req_duration p90 | 7631.9 ms |
| **http_req_duration p95** | 7805.5 ms |
| **http_req_duration p99** | 7914.1 ms |
| http_req_duration max | 7948.4 ms |
| http_req_waiting avg (TTFB) | 5692.0 ms |
| **http_req_failed rate** | 90.00% |
| 200 성공 | 200 |
| 200 실패 | 1800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 389.9 KB |
| data_sent | 305.6 KB |

## [2026-04-16 08:25] Gateway — atomic + async (2000 VU) #3

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 2000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 2000 |
| **TPS (req/s)** | 241.30 |
| iterations | 2000 |
| iteration_duration avg | 5660.4 ms |
| **http_req_duration avg** | 5567.8 ms |
| http_req_duration min | 1155.5 ms |
| http_req_duration p50 | 5874.7 ms |
| http_req_duration p90 | 7748.6 ms |
| **http_req_duration p95** | 7903.2 ms |
| **http_req_duration p99** | 8048.2 ms |
| http_req_duration max | 8143.8 ms |
| http_req_waiting avg (TTFB) | 5558.3 ms |
| **http_req_failed rate** | 90.00% |
| 200 성공 | 200 |
| 200 실패 | 1800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 393.8 KB |
| data_sent | 305.6 KB |

## [2026-04-16 08:31] Gateway — pessimistic + async (2000 VU) #2

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 2000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 2000 |
| **TPS (req/s)** | 237.34 |
| iterations | 2000 |
| iteration_duration avg | 5737.2 ms |
| **http_req_duration avg** | 5665.3 ms |
| http_req_duration min | 689.0 ms |
| http_req_duration p50 | 5939.9 ms |
| http_req_duration p90 | 7854.8 ms |
| **http_req_duration p95** | 8006.8 ms |
| **http_req_duration p99** | 8166.4 ms |
| http_req_duration max | 8204.6 ms |
| http_req_waiting avg (TTFB) | 5656.9 ms |
| **http_req_failed rate** | 90.00% |
| 200 성공 | 200 |
| 200 실패 | 1800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 389.9 KB |
| data_sent | 305.6 KB |

## [2026-04-16 08:32] Gateway — pessimistic + async (2000 VU) #3

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 2000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 2000 |
| **TPS (req/s)** | 203.51 |
| iterations | 2000 |
| iteration_duration avg | 6838.6 ms |
| **http_req_duration avg** | 6702.3 ms |
| http_req_duration min | 1307.7 ms |
| http_req_duration p50 | 7289.8 ms |
| http_req_duration p90 | 9118.1 ms |
| **http_req_duration p95** | 9343.8 ms |
| **http_req_duration p99** | 9558.3 ms |
| http_req_duration max | 9734.7 ms |
| http_req_waiting avg (TTFB) | 6695.4 ms |
| **http_req_failed rate** | 90.00% |
| 200 성공 | 200 |
| 200 실패 | 1800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 390.2 KB |
| data_sent | 305.6 KB |

## [2026-04-16 14:32] Actuator 메트릭 수집 검증 (2000 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 2000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 2000 |
| **TPS (req/s)** | 212.01 |
| iterations | 2000 |
| iteration_duration avg | 6797.6 ms |
| **http_req_duration avg** | 6699.8 ms |
| http_req_duration min | 1949.3 ms |
| http_req_duration p50 | 6914.8 ms |
| http_req_duration p90 | 8563.5 ms |
| **http_req_duration p95** | 8818.3 ms |
| **http_req_duration p99** | 9114.4 ms |
| http_req_duration max | 9346.5 ms |
| http_req_waiting avg (TTFB) | 6695.2 ms |
| **http_req_failed rate** | 90.00% |
| 200 성공 | 200 |
| 200 실패 | 1800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 389.6 KB |
| data_sent | 305.6 KB |

**서버 메트릭 (peak / avg)**

| 메트릭 | peak | avg |
|-------|------|-----|
| CPU process | 91.0% | 49.5% |
| CPU system | 90.9% | 47.4% |
| heap used (MB) | 99.8 | 70.9 |
| heap max (MB) | 494.9 | - |
| threads live | 219 | 154.0 |
| tomcat busy | - | - |
| tomcat current | - | - |
| hikari active | 10 | 2.5 |
| hikari pending | 187 | 46.8 |
| hikari idle | 10 | 7.5 |

## [2026-04-16 14:34] Actuator 메트릭 수집 검증 (2000 VU)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 2000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 2000 |
| **TPS (req/s)** | 166.30 |
| iterations | 2000 |
| iteration_duration avg | 7376.1 ms |
| **http_req_duration avg** | 7327.8 ms |
| http_req_duration min | 1297.9 ms |
| http_req_duration p50 | 7612.2 ms |
| http_req_duration p90 | 10726.3 ms |
| **http_req_duration p95** | 11201.7 ms |
| **http_req_duration p99** | 11639.8 ms |
| http_req_duration max | 11833.8 ms |
| http_req_waiting avg (TTFB) | 7321.5 ms |
| **http_req_failed rate** | 90.00% |
| 200 성공 | 200 |
| 200 실패 | 1800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 393.9 KB |
| data_sent | 305.6 KB |

**서버 메트릭 (peak / avg)**

| 메트릭 | peak | avg |
|-------|------|-----|
| CPU process | 89.6% | 46.1% |
| CPU system | 92.2% | 46.3% |
| heap used (MB) | 121.6 | 79.1 |
| heap max (MB) | 494.9 | - |
| threads live | 219 | 141.0 |
| hikari active | 10 | 2.0 |
| hikari pending | 171 | 64.0 |
| hikari idle | 10 | 8.0 |
| hikari timeout (total) | 0 | - |

> 시계열 CSV: `k6/metrics/run_20260416_233418_Actuator_메트릭_수집_검증__2000_VU_.csv`

## [2026-04-16 15:01] 서버 터뜨리기 관찰 (2000 VU, atomic+async)

| 항목 | 값 |
|------|-----|
| BASE_URL | http://localhost:9000 |
| 동시 VU | 2000 |
| 쿠폰 수량 | 200 |
| **총 요청 수** | 2000 |
| **TPS (req/s)** | 212.47 |
| iterations | 2000 |
| iteration_duration avg | 6345.5 ms |
| **http_req_duration avg** | 6236.6 ms |
| http_req_duration min | 1563.0 ms |
| http_req_duration p50 | 6609.8 ms |
| http_req_duration p90 | 8591.9 ms |
| **http_req_duration p95** | 8826.2 ms |
| **http_req_duration p99** | 9065.2 ms |
| http_req_duration max | 9246.5 ms |
| http_req_waiting avg (TTFB) | 6224.3 ms |
| **http_req_failed rate** | 90.00% |
| 200 성공 | 200 |
| 200 실패 | 1800 |
| totalQuantity | 200 |
| remainingQuantity | 0 |
| 실제 발급(DB) | 200 |
| 소비량(total-remaining) | 200 |
| **정합성** | PASS ✅ |
| data_received | 389.6 KB |
| data_sent | 305.6 KB |

**서버 메트릭 (peak / avg)**

| 메트릭 | peak | avg |
|-------|------|-----|
| CPU process | 95.9% | 35.7% |
| CPU system | 93.4% | 27.3% |
| heap used (MB) | 102.2 | 71.3 |
| heap max (MB) | 494.9 | - |
| threads live | 219 | 141.8 |
| hikari active | 7 | 1.4 |
| hikari pending | 186 | 37.2 |
| hikari idle | 10 | 8.6 |
| hikari timeout (total) | 0 | - |

> 시계열 CSV: `k6/metrics/run_20260417_000141_서버_터뜨리기_관찰__2000_VU,_atomic+async_.csv`
