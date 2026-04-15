# Feature: 셋업 API (유저 생성, 포인트 충전, 쿠폰 생성)

## 1. Problem Definition

- 부하 테스트(CouponLoadTest)를 실행하려면 유저 생성, 포인트 충전, 쿠폰 생성 API가 필요하다.
- 이 API들은 테스트 데이터 셋업 용도로, 순차 호출되며 동시성 문제와 무관하다.

## 2. Prerequisites

- ✅ User 엔티티 + UserRepository
- ✅ Coupon 엔티티 + CouponRepository
- ✅ 예외 클래스 (UserNotFoundException, InsufficientPointsException)
- ✅ 글로벌 예외 핸들러

## 3. Requirements

### Functional

- `POST /api/users` — email, name으로 유저 생성. 응답에 생성된 유저 정보 반환
- `POST /api/users/{userId}/points` — amount만큼 포인트 충전. 응답에 충전 후 유저 정보 반환
- `POST /api/coupons` — 쿠폰 생성. 응답에 생성된 쿠폰 정보 반환

### Non-functional

- 컨트롤러 URL, HTTP 메서드, 요청/응답 필드 변경 금지 (README 제약)
- 요청 DTO에 기본 validation 적용

## 4. API Design

### POST `/api/users`

```
Request:  { "email": "test@test.com", "name": "홍길동" }
Response: { "id": 1, "email": "test@test.com", "name": "홍길동", "point": 0 }
```

### POST `/api/users/{userId}/points`

```
Request:  { "amount": 10000 }
Response: { "id": 1, "email": "test@test.com", "name": "홍길동", "point": 10000 }
```

| 에러 | HTTP Status | 메시지 |
|------|-------------|--------|
| userId 미존재 | 404 | User not found |

### POST `/api/coupons`

```
Request:  {
    "name": "대박쿠폰",
    "description": "이벤트 오픈 기념",
    "pointCost": 100,
    "totalQuantity": 200,
    "startDate": "2026-04-14T00:00:00",
    "endDate": "2026-05-14T00:00:00"
}
Response: {
    "id": 1,
    "name": "대박쿠폰",
    "description": "이벤트 오픈 기념",
    "pointCost": 100,
    "totalQuantity": 200,
    "remainingQuantity": 200,
    "startDate": "2026-04-14T00:00:00",
    "endDate": "2026-05-14T00:00:00"
}
```

## 5. Domain Model (Draft)

### 패키지 구조 (추가분)

```
domain/user/
├── controller/
│   └── UserController.java
├── dto/
│   ├── CreateUserRequest.java
│   ├── DepositPointsRequest.java
│   └── UserResponse.java
└── service/
    ├── UserService.java
    └── PointService.java

domain/coupon/
├── controller/
│   └── CouponController.java
├── dto/
│   ├── CreateCouponRequest.java
│   └── CouponResponse.java
└── service/
    └── CouponService.java (셋업용 생성 메서드만, 발급은 다음 Epic)
```

### Service

```java
// UserService — 유저 생성, 포인트 충전 (셋업용)
@Service
public class UserService {
    User createUser(String email, String name) { ... }
    User depositPoints(Long userId, Long amount) { ... }
}

// PointService — 포인트 차감 (쿠폰 발급 시 CouponService에서 호출)
@Service
public class PointService {
    @Transactional
    public void decreasePoints(Long userId, Long amount) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.usePoints(amount);
    }
}
```

> PointService는 이 Epic에서 구현하되, 실제 호출은 다음 Epic(쿠폰 발급)에서 이루어진다.

### DTO

```java
// User DTOs
record CreateUserRequest(String email, String name) {}
record DepositPointsRequest(Long amount) {}
record UserResponse(Long id, String email, String name, Long point) {
    static UserResponse from(User user) { ... }
}

// Coupon DTOs
record CreateCouponRequest(
    String name, String description, Integer pointCost,
    Integer totalQuantity, LocalDateTime startDate, LocalDateTime endDate
) {}
record CouponResponse(
    Long id, String name, String description, Integer pointCost,
    Integer totalQuantity, Integer remainingQuantity,
    LocalDateTime startDate, LocalDateTime endDate
) {
    static CouponResponse from(Coupon coupon) { ... }
}
```

## 6. Implementation Steps

| Step | 작업 | 산출물 |
|------|------|--------|
| 1 | User DTO + UserService + UserController | 유저 생성 API |
| 2 | 포인트 충전 (UserService 메서드 + Controller 엔드포인트) | 포인트 충전 API |
| 3 | PointService — 포인트 차감 로직 | PointService.decreasePoints() |
| 4 | Coupon DTO + CouponService + CouponController | 쿠폰 생성 API |

## 7. TDD Plan

### Task 1: 유저 생성 API

1. **테스트 작성 (RED)**: UserService.createUser() — 유저 생성 후 저장된 유저 반환 검증
2. **최소 구현 (GREEN)**: UserService + CreateUserRequest + UserResponse
3. **테스트 작성 (RED)**: UserController POST `/api/users` — 200 응답 + JSON 필드 검증 (MockMvc)
4. **최소 구현 (GREEN)**: UserController 구현

### Task 2: 포인트 충전 API

1. **테스트 작성 (RED)**: UserService.depositPoints() — 포인트 누적 검증, 유저 미존재 시 예외 검증
2. **최소 구현 (GREEN)**: UserService.depositPoints 메서드
3. **테스트 작성 (RED)**: UserController POST `/api/users/{userId}/points` — 200 응답 + 404 에러 검증
4. **최소 구현 (GREEN)**: UserController 엔드포인트 추가

### Task 3: PointService (포인트 차감)

1. **테스트 작성 (RED)**: PointService.decreasePoints() — 정상 차감 검증
2. **테스트 작성 (RED)**: 유저 미존재 시 UserNotFoundException 검증
3. **테스트 작성 (RED)**: 잔액 부족 시 InsufficientPointsException 검증
4. **최소 구현 (GREEN)**: PointService 구현

### Task 4: 쿠폰 생성 API

1. **테스트 작성 (RED)**: CouponService.createCoupon() — 쿠폰 생성 후 remainingQuantity == totalQuantity 검증
2. **최소 구현 (GREEN)**: CouponService + CreateCouponRequest + CouponResponse
3. **테스트 작성 (RED)**: CouponController POST `/api/coupons` — 200 응답 + JSON 필드 검증
4. **최소 구현 (GREEN)**: CouponController 구현

## 8. Verification Criteria

- [ ] `./gradlew :coupon-api:test` 전체 통과
- [ ] 3개 API 엔드포인트가 spec v1.0 요청/응답 형식과 일치
- [ ] 유저 미존재 시 404 에러 응답 확인
- [ ] 패키지 구조가 domain/{user,coupon}/{controller,dto,service} 형태

## 9. Design Decisions

| 결정 | 근거 |
|------|------|
| DTO를 record로 정의 | Java 21 환경, 불변 데이터 전달 객체에 적합 |
| Service 레이어 분리 | Controller는 HTTP 매핑만, 비즈니스 로직은 Service에 집중 |
| CouponService에 생성만 구현 | 발급 로직은 다음 Epic(쿠폰 발급)에서 별도 구현 |
| PointService 별도 분리 | 포인트 차감은 쿠폰 발급 시 CouponService에서 호출하는 독립 책임. UserService(충전)와 분리 |
