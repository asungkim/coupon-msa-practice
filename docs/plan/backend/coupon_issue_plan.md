# Feature: 쿠폰 발급 API + 알림 연동

## 1. Problem Definition

- 선착순 쿠폰 발급의 핵심 엔드포인트로, 200명 동시 요청이 집중되는 유일한 지점이다.
- Phase 1에서는 **의도적으로 문제가 있는 구현**으로 만든다:
    - Java 레벨 `remainingQuantity--` → 동시성 lost update
    - 트랜잭션 내 알림 동기 호출 → 커넥션 고갈
- 이 문제들을 Phase 2~3에서 진단하고 해결하는 것이 학습 목표다.

## 2. Prerequisites

- ✅ User, Coupon, CouponIssue 엔티티 + Repository
- ✅ 예외 클래스 6종 + 글로벌 예외 핸들러
- ✅ PointService.decreasePoints()
- ✅ CouponService (생성 메서드)
- ✅ CouponController (생성 엔드포인트)

## 3. Requirements

### Functional

- `POST /api/coupons/{couponId}/issue` — 쿠폰 발급 요청
- 발급 검증 순서: 중복 발급 → 유저 존재 → 쿠폰 존재 → 발급 가능(재고+기간) → 포인트 잔액
- 발급 처리 순서: 포인트 차감 → 재고 차감 → 발급 이력 저장 → 알림 전송
- 알림은 PushNotificationClient가 notification-mock-api에 동기 호출

### Non-functional

- Phase 1: 전체 흐름을 하나의 `@Transactional`로 처리 (의도적 문제)
- Phase 1: 재고 차감은 Java 레벨 `--` (의도적 문제)
- 알림 실패 시에도 쿠폰 발급은 롤백하지 않는다

## 4. API Design

### POST `/api/coupons/{couponId}/issue`

```
Request:  { "userId": 1 }
Response: {
    "id": 1,
    "userId": 1,
    "couponId": 1,
    "status": "ISSUED",
    "issuedAt": "2026-04-15T12:00:00"
}
```

### 에러 케이스

| 상황 | HTTP Status | 메시지 |
|------|-------------|--------|
| 유저 미존재 | 404 | User not found |
| 쿠폰 미존재 | 404 | Coupon not found |
| 중복 발급 | 409 | Coupon already issued to this user |
| 재고 소진 | 409 | Coupon out of stock |
| 기간 외/불가 | 400 | Coupon is not available or expired |
| 포인트 부족 | 400 | Insufficient points |

## 5. Domain Model (Draft)

### 패키지 구조 (추가분)

```
domain/coupon/
├── dto/
│   ├── IssueCouponRequest.java      (신규)
│   └── CouponIssueResponse.java     (신규)
├── service/
│   └── CouponService.java           (issueCoupon 메서드 추가)
└── controller/
    └── CouponController.java        (발급 엔드포인트 추가)

domain/coupon/
└── client/
    └── PushNotificationClient.java  (신규)
```

### DTO

```java
record IssueCouponRequest(Long userId) {}

record CouponIssueResponse(Long id, Long userId, Long couponId,
                           String status, LocalDateTime issuedAt) {
    static CouponIssueResponse from(CouponIssue issue) { ... }
}
```

### CouponService.issueCoupon (INITIAL_CODE 기준)

```java
@Transactional
public CouponIssueResponse issueCoupon(Long userId, Long couponId) {
    // 1. 중복 발급 검사
    if (couponIssueRepository.existsByUserIdAndCouponId(userId, couponId))
        throw new CouponAlreadyIssuedException(userId, couponId);

    // 2. 유저 존재 검사
    var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

    // 3. 쿠폰 존재 검사
    var coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CouponNotFoundException(couponId));

    // 4. 발급 가능 검사
    if (!coupon.isAvailable())
        throw new CouponNotAvailableException(couponId, "not available or expired");

    // 5. 포인트 차감
    pointService.decreasePoints(userId, (long) coupon.getPointCost());

    // 6. 재고 차감 (Java 레벨 — Phase 2에서 개선)
    coupon.decreaseQuantity();

    // 7. 발급 이력 저장
    var issue = couponIssueRepository.save(
            new CouponIssue(userId, couponId, IssueStatus.ISSUED));

    // 8. 알림 전송 (트랜잭션 내 동기 — Phase 3에서 개선)
    pushNotificationClient.sendCouponIssuedNotification(
            userId, coupon.getName(), coupon.getId());

    return CouponIssueResponse.from(issue);
}
```

### PushNotificationClient (INITIAL_CODE 기준)

```java
@Component
public class PushNotificationClient {
    private final WebClient webClient;  // notification-mock-api 연결

    public void sendCouponIssuedNotification(Long userId, String couponName, Long couponId) {
        // WebClient로 POST /api/push/send 동기 호출 (.block())
    }
}
```

## 6. Implementation Steps

| Step | 작업 | 산출물 |
|------|------|--------|
| 1 | IssueCouponRequest, CouponIssueResponse DTO | DTO 2개 |
| 2 | CouponService.issueCoupon — 발급 로직 | Service 메서드 |
| 3 | CouponController — 발급 엔드포인트 | Controller 엔드포인트 |
| 4 | PushNotificationClient | 알림 클라이언트 |

## 7. TDD Plan

### Task 1: CouponService.issueCoupon — 정상 발급

1. **테스트 작성 (RED)**: 정상 발급 시 CouponIssueResponse 반환, status == ISSUED, 재고 1 감소, 포인트 차감 호출 검증
2. **최소 구현 (GREEN)**: issueCoupon 메서드 (알림 제외)

### Task 2: CouponService.issueCoupon — 실패 케이스

1. **테스트 작성 (RED)**: 중복 발급 시 CouponAlreadyIssuedException
2. **최소 구현 (GREEN)**: existsBy 검사 추가
3. **테스트 작성 (RED)**: 유저 미존재 시 UserNotFoundException
4. **테스트 작성 (RED)**: 쿠폰 미존재 시 CouponNotFoundException
5. **테스트 작성 (RED)**: 발급 불가(재고 0 or 기간 외) 시 CouponNotAvailableException
6. **테스트 작성 (RED)**: 포인트 부족 시 InsufficientPointsException

### Task 3: CouponController — 발급 엔드포인트

1. **테스트 작성 (RED)**: POST `/api/coupons/{couponId}/issue` — 200 정상 응답 검증
2. **테스트 작성 (RED)**: 각 에러 케이스별 HTTP Status 검증 (409, 404, 400)
3. **최소 구현 (GREEN)**: Controller 엔드포인트

### Task 4: PushNotificationClient

1. **테스트 작성 (RED)**: 알림 전송 호출 시 WebClient가 올바른 요청을 보내는지 검증
2. **최소 구현 (GREEN)**: PushNotificationClient 구현
3. **CouponService에 주입**: issueCoupon 마지막에 알림 호출 추가

## 8. Verification Criteria

- [ ] `./gradlew :coupon-api:test` 전체 통과
- [ ] 발급 API 정상 응답이 spec v1.0 형식과 일치
- [ ] 6가지 에러 케이스 각각 올바른 HTTP Status 반환
- [ ] 발급 시 포인트 차감 + 재고 차감 + 발급 이력 저장이 하나의 트랜잭션
- [ ] 알림이 트랜잭션 내에서 동기 호출됨 (Phase 1 의도)

## 9. Design Decisions

| 결정 | 근거 |
|------|------|
| issueCoupon을 기존 CouponService에 추가 | 쿠폰 도메인 책임. 별도 IssueCouponService 분리는 과도 |
| PushNotificationClient를 coupon/client 패키지에 배치 | 쿠폰 발급 시에만 사용하는 외부 연동 클라이언트 |
| 알림을 @Transactional 안에서 동기 호출 | Phase 1 의도적 문제. Phase 3에서 이벤트 기반으로 분리 예정 |
| INITIAL_CODE의 orElse(null) + null 체크 대신 orElseThrow 사용 | 더 안전한 패턴. 동작은 동일 |
| CouponService에 UserRepository 직접 주입 | INITIAL_CODE 구조 준수. 발급 검증 시 유저 조회 필요 |
