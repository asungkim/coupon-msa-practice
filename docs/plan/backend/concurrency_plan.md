# Feature: 재고 동시성 해결 (비관적 락 vs 원자적 UPDATE)

## 1. Problem Definition

- 현재 `coupon.decreaseQuantity()`가 Java 레벨 `--`로 처리됨
- 300명 동시 요청 시 read → modify → write 사이에 다른 트랜잭션이 같은 값을 읽고 덮어씀 (lost update)
- Step 2 부하 테스트 결과: 300건 발급 / 53건만 차감 (remainingQuantity 147)
- 서버 N대, DB 1대 환경 → DB가 유일한 동기화 지점
- HikariCP 10개 제약 → 트랜잭션을 최대한 짧게 해서 커넥션 회전을 빠르게 해야 함

## 2. Prerequisites

- ✅ 알림 분리 완료 (비동기, 트랜잭션 외부)
- ✅ 부하 테스트 기반 정비 (조회 API, DB 정합성 검증, 결과 파일 저장)
- ✅ Step 2 baseline: 300명 전원 성공 / 0.5초 / 정합성 FAIL

## 3. 설계 근거

### 왜 DB 수준인가?

- 서버가 2대 이상 → JVM 내 synchronized/Lock은 자기 서버만 보호
- 모든 서버가 공유하는 유일한 지점이 DB → DB lock이 유일한 해법 (외부 인프라 없이)

### 비관적 락 vs 원자적 UPDATE

| | 비관적 락 | 원자적 UPDATE |
|---|---------|-------------|
| 방식 | SELECT FOR UPDATE → Java 검증 → Java 수정 → COMMIT | UPDATE WHERE qty > 0 한 문장 |
| lock 구간 | SELECT ~ COMMIT 전체 | UPDATE 실행 순간만 |
| JPA 친화성 | `@Lock(PESSIMISTIC_WRITE)` 사용 | `@Modifying @Query` 직접 작성 |
| 복합 검증 | lock 잡은 상태에서 자유롭게 | UPDATE WHERE 조건에 녹이거나 사전 체크 |
| 커넥션 회전 | 느림 (lock 대기 큐) | 빠름 (즉시 반환) |

### HikariCP 10개로 300명 처리

- 동시에 10개 트랜잭션만 실행 가능 → 나머지 290명은 커넥션 대기
- 한 트랜잭션이 1ms면 300명 = 30회전 = 30ms
- 한 트랜잭션이 50ms면 300명 = 30회전 = 1.5초
- **트랜잭션 시간 최소화 = 처리량 극대화**

## 4. 구현 계획

두 전략을 모두 구현하고 설정값으로 전환하여 동일 부하 테스트로 비교한다.

### 전략 A: 비관적 락

```java
// CouponRepository에 추가
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Coupon c WHERE c.id = :id")
Optional<Coupon> findByIdForUpdate(@Param("id") Long id);
```

발급 흐름:
1. `findByIdForUpdate(couponId)` → row lock 획득
2. `coupon.isAvailable()` 검증
3. `coupon.decreaseQuantity()` Java에서 차감
4. JPA dirty checking으로 UPDATE
5. COMMIT → lock 해제

### 전략 B: 원자적 UPDATE

```java
// CouponRepository에 추가
@Modifying
@Query("UPDATE Coupon c SET c.remainingQuantity = c.remainingQuantity - 1 " +
       "WHERE c.id = :id AND c.remainingQuantity > 0")
int decreaseQuantity(@Param("id") Long id);
```

발급 흐름:
1. `couponRepository.findById(couponId)` → 일반 조회 (lock 없음)
2. `coupon.isAvailable()` 사전 검증 (기간 체크 등)
3. `couponRepository.decreaseQuantity(couponId)` → 원자적 차감
4. affected rows == 0이면 재고 소진 예외
5. COMMIT

### 전략 전환

```yaml
coupon:
  issue:
    strategy: pessimistic  # pessimistic | atomic
```

CouponService에서 strategy 값에 따라 분기하거나, 전략 패턴으로 구현체를 분리.

## 5. 패키지 구조 (변경분)

```
domain/coupon/
├── repository/
│   └── CouponRepository.java         (수정 — findByIdForUpdate, decreaseQuantity 추가)
├── service/
│   ├── CouponService.java            (수정 — 전략에 따라 분기)
│   └── strategy/
│       ├── CouponIssueStrategy.java   (인터페이스)
│       ├── PessimisticLockIssueStrategy.java
│       └── AtomicUpdateIssueStrategy.java
```

## 6. 추가 고려사항

### UK 위반 예외 처리

동시 요청 시 `existsByUserIdAndCouponId` 체크를 둘 다 통과 → 둘 다 insert → UK 위반:
- `DataIntegrityViolationException` → GlobalExceptionHandler에서 409로 변환

### 부하 테스트 확장

- 기존: 300명 / 200쿠폰
- 추가: 500명 / 200쿠폰, 1000명 / 200쿠폰 등 규모를 키워 비교
- LOAD_TEST_USERS, LOAD_TEST_QUANTITY 환경변수로 조절 가능하게

## 7. 성능 비교 계획

| 측정 항목 | Step 2 (현재) | 전략 A (비관적) | 전략 B (원자적) |
|----------|-------------|---------------|---------------|
| HTTP 200 성공 수 | 300 | ? | ? |
| HTTP 4xx 실패 수 | 0 | ? | ? |
| 총 소요 시간 | 0.5초 | ? | ? |
| remainingQuantity | 147 | ? | ? |
| 실제 발급 건수(DB) | 300 | ? | ? |
| 정합성 | FAIL | ? | ? |

> 추가로 500명, 1000명 테스트 결과도 기록

## 8. TDD Plan

### Task 1: CouponRepository 락 메서드 추가

1. **테스트 작성 (RED)**: findByIdForUpdate가 Coupon을 반환하는지 검증
2. **최소 구현 (GREEN)**: @Lock(PESSIMISTIC_WRITE) 메서드 추가
3. **테스트 작성 (RED)**: decreaseQuantity가 affected rows 1을 반환하는지, qty=0이면 0 반환 검증
4. **최소 구현 (GREEN)**: @Modifying @Query 메서드 추가

### Task 2: 전략 패턴 구현

1. CouponIssueStrategy 인터페이스 정의
2. PessimisticLockIssueStrategy 구현 (findByIdForUpdate → decreaseQuantity → save)
3. AtomicUpdateIssueStrategy 구현 (findById → decreaseQuantity → affected rows 체크)
4. CouponService에서 strategy 설정에 따라 구현체 선택

### Task 3: UK 위반 예외 처리

1. **테스트 작성 (RED)**: DataIntegrityViolationException 발생 시 409 응답 검증
2. **최소 구현 (GREEN)**: GlobalExceptionHandler에 핸들러 추가

### Task 4: 부하 테스트 확장 + 실행

1. CouponLoadTest에 LOAD_TEST_USERS, LOAD_TEST_QUANTITY 환경변수 지원
2. 전략 A (비관적 락) — 300명/200쿠폰 부하 테스트 + 결과 기록
3. 전략 B (원자적 UPDATE) — 300명/200쿠폰 부하 테스트 + 결과 기록
4. 규모 확장 테스트 (500명, 1000명)
5. 전체 비교표 작성

## 9. Verification Criteria

- [ ] `./gradlew :coupon-api:test` 전체 통과
- [ ] 전략 A: 300명/200쿠폰에서 정합성 PASS (발급 <= 200, 소비량 == 발급건수)
- [ ] 전략 B: 300명/200쿠폰에서 정합성 PASS
- [ ] UK 위반 시 409 응답
- [ ] 전략 A vs B 성능 비교표 작성
- [ ] 결과가 `docs/history/load_test_results.md`에 누적 기록

## 10. Design Decisions

| 결정 | 근거 |
|------|------|
| 전략 패턴으로 분리 | 설정 전환만으로 비관적/원자적 비교 가능. 코드 제거 없이 둘 다 유지 |
| @ConditionalOnProperty로 구현체 선택 | 알림 클라이언트와 동일한 패턴. 일관성 |
| 부하 테스트 규모를 환경변수로 조절 | 300/500/1000명 등 다양한 규모에서 비교 |
| UK 위반 예외를 GlobalExceptionHandler에서 처리 | 동시 요청 시 existsBy 체크를 둘 다 통과하는 race condition 대응 |
