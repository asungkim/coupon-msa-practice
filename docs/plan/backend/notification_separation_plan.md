# Feature: 알림 분리 (트랜잭션 외부 + 동기/비동기 비교)

## 1. Problem Definition

- 현재 `PushNotificationClient.sendCouponIssuedNotification()`이 `@Transactional` 안에서 동기 호출(`.block()`)됨
- 알림 서버 응답(2~3초) 동안 DB 커넥션을 점유 → HikariCP 10개 고갈
- 알림 실패 시 쿠폰 발급까지 롤백 — 부가 기능이 핵심 기능을 망가뜨리는 구조
- Phase 1 부하 테스트 결과: 300명 요청 중 114건만 성공, 30초 소요

## 2. Prerequisites

- ✅ Phase 1 전체 완료 (쿠폰 발급 + 알림 동기 호출)
- ✅ PushNotificationClient (WebClient + `.block()`)
- ✅ 부하 테스트 (CouponLoadTest)

## 2.1 선행 작업: 부하 테스트 기반 정비

알림 분리 전에, 각 단계별 성능을 수치로 비교할 수 있는 기반을 먼저 갖춘다.

### 조회 API 추가

- `GET /api/coupons/{couponId}` — 쿠폰 정보 (remainingQuantity 포함)
- 발급 건수 조회 — CouponIssueRepository.countByCouponId()

### 부하 테스트 리팩토링

- 부하 테스트 종료 후 조회 API로 DB 상태 검증:
  - remainingQuantity
  - 실제 발급 건수
  - `totalQuantity - remainingQuantity == 발급 건수` 정합성 비교
- 결과를 `docs/history/load_test_results.md`에 마크다운으로 누적 기록
- 각 단계(Phase 1 / Step 1 / Step 2)마다 같은 포맷으로 기록하여 비교 가능

### Phase 1 baseline 기록

- 선행 작업 완료 후 부하 테스트를 다시 돌려 Phase 1 baseline 결과를 기록

## 3. 설계 근거

### 왜 트랜잭션 밖으로 분리하는가?

- 알림은 "발급됐다는 사실을 알려주는 것"이지 발급의 조건이 아님
- 알림 실패 시 발급을 롤백해야 하는가? → No → 트랜잭션에 포함될 이유 없음
- 외부 API를 트랜잭션 안에서 호출하는 것 자체가 anti-pattern

### 왜 이벤트 기반인가?

- 커밋 확정 후 알림을 보내야 함 (미커밋 상태에서 알림이 나가면 안 됨)
- `@TransactionalEventListener(phase = AFTER_COMMIT)` → 커밋 후 실행 보장
- 비동기 즉시 호출(`@Async`)은 커밋 전에 알림이 갈 수 있어서 부적합

### 동기 vs 비동기 — 왜 둘 다 하는가?

- Step 1 (동기): 트랜잭션 분리만으로 커넥션 고갈이 해결되는지 확인
- Step 2 (비동기): 리스너 스레드 블로킹까지 제거했을 때 추가 개선 폭 확인
- 각 단계별 부하 테스트 수치 비교가 학습 목표

## 4. 구현 계획

### Step 1: 트랜잭션 분리 + 동기 호출

**변경 흐름:**

```
[Before — Phase 1]
@Transactional {
    검증 → 포인트 차감 → 재고 차감 → 이력 저장 → 알림 동기 호출(2~3초)
}  ← 여기서야 커넥션 반환

[After — Step 1]
@Transactional {
    검증 → 포인트 차감 → 재고 차감 → 이력 저장 → 이벤트 발행
}  ← 커넥션 즉시 반환

@TransactionalEventListener(AFTER_COMMIT)
→ 알림 동기 호출(2~3초)  ← DB 커넥션 없이 실행
```

**변경 사항:**
- CouponService: `pushNotificationClient` 직접 호출 → `ApplicationEventPublisher.publishEvent()`
- CouponIssuedEvent: 이벤트 객체 (userId, couponName, couponId)
- CouponIssuedEventListener: `@TransactionalEventListener(phase = AFTER_COMMIT)` + 동기 알림 호출
- PushNotificationClient: WebClient `.block()` → `RestClient` 동기 호출로 교체 (목적에 맞게)

**기대 효과:**
- DB 커넥션 점유 시간: 2~3초 → 수십ms
- 알림 실패 시 발급 롤백 안 됨
- 리스너 스레드는 여전히 2~3초 블로킹

### Step 2: 동기 → 비동기 전환

**변경 사항:**
- PushNotificationClient: `RestClient` 동기 → `WebClient` 비동기 (`.subscribe()`)
- 리스너에서 호출 후 즉시 반환 (블로킹 없음)

**기대 효과:**
- 리스너 스레드 블로킹 제거
- 200명 동시 발급 시 알림도 논블로킹으로 동시 처리
- 서버 스레드 풀 부담 감소

## 5. 패키지 구조 (변경분)

```
domain/coupon/
├── event/
│   ├── CouponIssuedEvent.java          (신규)
│   └── CouponIssuedEventListener.java  (신규)
├── client/
│   └── PushNotificationClient.java     (수정)
└── service/
    └── CouponService.java              (수정 — 직접 호출 → 이벤트 발행)
```

## 6. 성능 비교 계획

각 단계에서 동일한 부하 테스트(300명/200쿠폰)를 실행하여 비교:

| 측정 항목 | Phase 1 (현재) | Step 1 (동기) | Step 2 (비동기) |
|----------|---------------|--------------|----------------|
| HTTP 200 성공 수 | 114 | ? | ? |
| 총 소요 시간 | 30초 | ? | ? |
| 커넥션 고갈 여부 | Yes | ? | ? |
| remainingQuantity | ? | ? | ? |
| 실제 발급 건수(DB) | ? | ? | ? |
| 정합성 일치 여부 | ? | ? | ? |

> 결과는 `docs/history/load_test_results.md`에 누적 기록

## 7. TDD Plan

### Task 0: 선행 — 조회 API + 부하 테스트 리팩토링

1. GET `/api/coupons/{couponId}` 조회 API 추가
2. CouponIssueRepository.countByCouponId() 추가
3. 부하 테스트에 DB 정합성 검증 추가 (조회 API 호출)
4. 결과를 `docs/history/load_test_results.md`에 저장하도록 리팩토링
5. Phase 1 baseline 결과 기록

### Task 1: 이벤트 객체 + 리스너 (Step 1)

1. **테스트 작성 (RED)**: CouponService.issueCoupon 호출 시 이벤트가 발행되는지 검증
2. **최소 구현 (GREEN)**: CouponIssuedEvent + ApplicationEventPublisher 주입 + publishEvent 호출
3. **테스트 작성 (RED)**: CouponIssuedEventListener가 이벤트 수신 후 PushNotificationClient를 호출하는지 검증
4. **최소 구현 (GREEN)**: EventListener + @TransactionalEventListener(AFTER_COMMIT)

### Task 2: PushNotificationClient 동기 전환 (Step 1)

1. **테스트 작성 (RED)**: RestClient 기반 동기 호출 테스트
2. **최소 구현 (GREEN)**: WebClient `.block()` → RestClient로 교체
3. CouponService에서 pushNotificationClient 직접 호출 제거

### Task 3: Step 1 부하 테스트

1. 서버 기동 → 부하 테스트 실행
2. Phase 1 결과와 비교
3. 커넥션 고갈 해소 여부 확인

### Task 4: 비동기 전환 (Step 2)

1. **테스트 작성 (RED)**: 비동기 호출 시 리스너가 블로킹하지 않는지 검증
2. **최소 구현 (GREEN)**: RestClient → WebClient `.subscribe()` 전환
3. 부하 테스트 실행 + Step 1 결과와 비교

## 8. Verification Criteria

- [ ] `./gradlew :coupon-api:test` 전체 통과
- [ ] 알림이 트랜잭션 커밋 후에 전송됨 (AFTER_COMMIT)
- [ ] 알림 실패 시 쿠폰 발급은 유지됨
- [ ] Step 1 부하 테스트: 커넥션 고갈 해소, 성공 수 개선
- [ ] Step 2 부하 테스트: Step 1 대비 추가 개선 확인
- [ ] Phase 1 / Step 1 / Step 2 성능 비교표 작성

## 9. Design Decisions

| 결정 | 근거 |
|------|------|
| `@TransactionalEventListener(AFTER_COMMIT)` 사용 | 커밋 확정 후 알림 전송 보장. `@Async`는 커밋 전 실행 가능성 |
| Step 1에서 RestClient 사용 | 동기 HTTP 클라이언트에 WebClient `.block()`은 과도. 목적에 맞는 도구 선택 |
| Step 2에서 WebClient `.subscribe()` 사용 | 논블로킹 비동기 호출. 리스너 스레드 즉시 반환 |
| Outbox 패턴 미적용 | Phase 2는 외부 인프라 없이 해결. 알림 유실이 치명적이지 않은 상황. 필요 시 Phase 3에서 도입 |
| 이벤트 객체를 coupon/event 패키지에 배치 | 쿠폰 발급 도메인에 속하는 이벤트. 리스너도 같은 패키지 |
