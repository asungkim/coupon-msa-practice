# Feature: Phase 1 테스트 (단위 + 통합 + 부하)

## 1. Problem Definition

- Phase 1 기능 구현이 완료되었으나, 전체 흐름을 실제 Spring Context에서 검증하는 통합 테스트가 없다.
- 부하 테스트(200명 동시 발급)로 Phase 1의 의도적 문제(lost update, 커넥션 고갈)를 실제로 확인해야 한다.
- 단위 테스트는 이미 각 Epic에서 작성했으므로, 누락 여부만 점검한다.

## 2. Prerequisites

- ✅ 모든 Phase 1 Epic 완료 (엔티티, 셋업 API, 쿠폰 발급, 알림 연동)
- ✅ notification-mock-api 모듈 구현 완료

## 3. Requirements

### Functional

- 통합 테스트: 발급 API의 전체 흐름을 실제 DB(H2 인메모리)로 검증
  - 정상 발급: 유저 생성 → 포인트 충전 → 쿠폰 생성 → 발급 → 응답 확인
  - 실패 케이스: 중복 발급, 재고 소진, 포인트 부족
- 부하 테스트: INITIAL_CODE의 CouponLoadTest 구현
  - 200명 동시 발급 요청
  - 결과 집계 (성공/실패 수, 소요 시간)
  - **이 테스트는 서버 기동 상태에서 실행하는 외부 테스트**

### Non-functional

- 통합 테스트는 `application-test.yml` 프로파일로 H2 인메모리 사용
- 부하 테스트는 `@Disabled` 또는 별도 프로파일로 일반 `./gradlew test`에서 제외
- 부하 테스트에서 Phase 1의 문제를 눈으로 확인할 수 있어야 함

## 4. 테스트 범위 분석

### 이미 작성된 단위 테스트 (점검만)

| 테스트 클래스 | 케이스 수 | 대상 |
|-------------|----------|------|
| UserTest | 4 | 엔티티 비즈니스 로직 |
| CouponTest | 7 | 엔티티 비즈니스 로직 |
| CouponIssueTest | 1 | 엔티티 생성 |
| CouponIssueRepositoryTest | 3 | Repository + UNIQUE 제약 |
| UserServiceTest | 3 | 서비스 단위 |
| PointServiceTest | 3 | 서비스 단위 |
| CouponServiceTest | 7 | 서비스 단위 (발급 포함) |
| UserControllerTest | 3 | 컨트롤러 단위 |
| CouponControllerTest | 7 | 컨트롤러 단위 (발급 포함) |
| GlobalExceptionHandlerTest | 6 | 예외 핸들러 |

**합계: 44개** — 단위 테스트는 충분.

### 신규 작성 대상

| 종류 | 테스트 클래스 | 설명 |
|------|-------------|------|
| 통합 | CouponIssueIntegrationTest | 실제 DB로 전체 발급 흐름 검증 |
| 부하 | CouponLoadTest | 200명 동시 요청 (서버 기동 필요, 일반 test에서 제외) |

## 5. Implementation Steps

| Step | 작업 | 산출물 |
|------|------|--------|
| 1 | 기존 단위 테스트 점검 — 누락 없음 확인 | - |
| 2 | 통합 테스트 작성 (발급 정상 + 실패 케이스) | CouponIssueIntegrationTest |
| 3 | 부하 테스트 작성 (INITIAL_CODE 기준) | CouponLoadTest |

## 6. TDD Plan

### Task 1: 통합 테스트 — 발급 정상 흐름

1. **테스트 작성**: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
2. 유저 생성 API → 포인트 충전 API → 쿠폰 생성 API → 발급 API 순서로 호출
3. 발급 후 응답 검증 (status == ISSUED, userId, couponId)
4. PushNotificationClient는 `@MockBean`으로 격리 (외부 서버 불필요)

### Task 2: 통합 테스트 — 발급 실패 케이스

1. **중복 발급**: 같은 유저가 같은 쿠폰 2번 발급 → 409
2. **재고 소진**: totalQuantity=1 쿠폰, 2명이 발급 → 두 번째 요청 409
3. **포인트 부족**: 포인트 0인 유저가 발급 → 400

### Task 3: 부하 테스트 (CouponLoadTest)

`@Disabled("서버 기동 후 수동 실행")` — 서버가 떠 있는 상태에서 WebClient로 외부 호출하는 테스트.

**시나리오**: 쿠폰 수량 200개, 유저 300명이 동시 발급 요청

**검증 항목**:

| # | 검증 포인트 | 기대 (정상) | Phase 1 예상 |
|---|-----------|-----------|-------------|
| 1 | 초과 발급 방지 | 성공 == 200, 실패 == 100 | 성공 > 200 (초과 발급 발생) |
| 2 | 중복 발급 방지 | 유저당 최대 1매 (UK 보장) | UK 위반 예외로 일부 실패 가능하나 중복 데이터는 없음 |
| 3 | 재고 정합성 | `totalQuantity - remainingQuantity == 실제 발급 건수` | 불일치 (lost update) |
| 4 | Lost update 검출 | remainingQuantity >= 0 | remainingQuantity가 음수이거나, 발급 건수와 불일치 |
| 5 | 소요 시간 | 10초 이내 | 알림 동기 호출(2~3초)로 인해 대폭 초과 가능 |

**테스트 흐름**:

1. 유저 300명 생성 + 포인트 충전 (순차)
2. 쿠폰 1개 생성 (totalQuantity=200)
3. 300명 동시 발급 요청 (CountDownLatch + ThreadPool)
4. 결과 집계:
   - HTTP 200 성공 수
   - DB에서 실제 CouponIssue 건수 조회 (GET API 또는 직접 카운트)
   - DB에서 Coupon.remainingQuantity 조회
   - `totalQuantity - remainingQuantity` vs 실제 발급 건수 비교
5. 결과 출력 (콘솔)

**Phase 1에서 예상되는 문제 출력 예시**:

```
========== 부하테스트 결과 ==========
동시 요청 수: 300 (쿠폰 수량: 200)
HTTP 200 성공: 243  ← 초과 발급!
실제 발급 건수(DB): 243
remainingQuantity: -43  ← 음수!
정합성: totalQuantity(200) - remainingQuantity(-43) = 243 != 200  ← 불일치!
소요시간: 15.2초  ← 커넥션 고갈로 느림
==========================================
```

## 7. Verification Criteria

- [ ] `./gradlew :coupon-api:test` 전체 통과 (부하 테스트는 @Disabled로 제외)
- [ ] 통합 테스트에서 발급 정상 흐름이 실제 DB에서 동작 확인
- [ ] 통합 테스트에서 실패 케이스별 올바른 HTTP Status 반환
- [ ] 부하 테스트 수동 실행 시 아래 문제를 콘솔 출력으로 확인 가능:
  - 초과 발급 (성공 > 200)
  - 재고 정합성 불일치 (`totalQuantity - remainingQuantity != 발급 건수`)
  - 소요 시간 초과

## 8. Design Decisions

| 결정 | 근거 |
|------|------|
| 통합 테스트에서 PushNotificationClient를 @MockBean으로 격리 | 외부 알림 서버 없이 테스트 가능. 알림 자체는 단위 테스트에서 검증 |
| 부하 테스트를 @Disabled로 처리 | 서버 기동이 필요한 외부 테스트이므로 일반 CI에서 제외 |
| 300명 요청 / 200개 쿠폰으로 설계 | 초과 발급 여부를 명확히 확인하기 위해 요청 > 수량 |
| 부하 테스트에서 DB 상태도 검증 | HTTP 성공 수만으로는 정합성 판단 불가. remainingQuantity와 실제 건수를 비교해야 lost update 검출 |
