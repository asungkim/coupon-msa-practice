# Feature: 엔티티 및 기반 구조

## 1. Problem Definition

- 쿠폰 발급 시스템의 도메인 모델(User, Coupon, CouponIssue)과 공통 예외 처리가 없으면 이후 모든 API 구현이 불가능하다.
- 이 Epic은 Phase 1의 첫 번째 작업으로, 셋업 API와 쿠폰 발급 API의 기반이 된다.

## 2. Prerequisites

- coupon-api 모듈 기본 구조 (Spring Boot, JPA, H2 설정) — 완료
- `docs/design/entity/v1.0.md`, `docs/spec/v1.0.md` — 완료

## 3. Requirements

### Functional

- User 엔티티: id, email, name, point 필드. 포인트 충전(deposit) 및 차감(usePoints) 메서드
- Coupon 엔티티: id, name, description, pointCost, totalQuantity, remainingQuantity, startDate, endDate 필드. 발급 가능 검사(isAvailable), 재고 차감(decreaseQuantity) 메서드
- CouponIssue 엔티티: id, userId, couponId, status, issuedAt 필드. (userId, couponId) UNIQUE 제약
- IssueStatus Enum: ISSUED, USED, EXPIRED
- 각 엔티티의 JpaRepository
- 비즈니스 예외 클래스 6종
- 글로벌 예외 핸들러 (@RestControllerAdvice)

### Non-functional

- ddl-auto: create-drop으로 스키마 자동 생성 (별도 마이그레이션 없음)
- 엔티티 비즈니스 로직에 대한 단위 테스트 작성

## 4. API Design (Draft)

이 Epic에서는 API 엔드포인트를 구현하지 않는다. 엔티티와 기반 구조만 구축한다.

에러 응답 형식 (글로벌 예외 핸들러):

```json
{
  "status": 404,
  "message": "User not found"
}
```

### 에러 케이스

| 상황 | 예외 클래스 | HTTP Status | 메시지 |
|------|-----------|-------------|--------|
| 유저 미존재 | UserNotFoundException | 404 | User not found |
| 쿠폰 미존재 | CouponNotFoundException | 404 | Coupon not found |
| 중복 발급 | CouponAlreadyIssuedException | 409 | Coupon already issued to this user |
| 재고 소진 | CouponOutOfStockException | 409 | Coupon out of stock |
| 기간 외/불가 | CouponNotAvailableException | 400 | Coupon is not available or expired |
| 포인트 부족 | InsufficientPointsException | 400 | Insufficient points |

## 5. Domain Model (Draft)

### 패키지 구조

```
com.example.coupon
├── domain
│   ├── user
│   │   ├── User.java
│   │   └── UserRepository.java
│   ├── coupon
│   │   ├── Coupon.java
│   │   ├── CouponRepository.java
│   │   ├── CouponIssue.java
│   │   ├── CouponIssueRepository.java
│   │   └── IssueStatus.java
│   └── exception
│       ├── UserNotFoundException.java
│       ├── CouponNotFoundException.java
│       ├── CouponAlreadyIssuedException.java
│       ├── CouponOutOfStockException.java
│       ├── CouponNotAvailableException.java
│       └── InsufficientPointsException.java
└── global
    └── exception
        └── GlobalExceptionHandler.java
```

### Entity

```java
@Entity
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;       // NOT NULL
    private String name;        // NOT NULL
    private Long point = 0L;    // NOT NULL, default 0

    // 포인트 충전
    public void depositPoints(Long amount) { ... }
    // 포인트 차감 (부족 시 InsufficientPointsException)
    public void usePoints(Long amount) { ... }
}

@Entity
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private Integer pointCost;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // 생성 시 remainingQuantity = totalQuantity
    // 발급 가능 여부 판단
    public boolean isAvailable() { ... }
    // 재고 차감 (0이면 CouponOutOfStockException)
    public void decreaseQuantity() { ... }
}

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
    name = "uk_coupon_issue_user_coupon",
    columnNames = {"userId", "couponId"}
))
public class CouponIssue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long couponId;
    @Enumerated(EnumType.STRING)
    private IssueStatus status;
    private LocalDateTime issuedAt;
}
```

### Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {}

public interface CouponRepository extends JpaRepository<Coupon, Long> {}

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
```

## 6. Implementation Steps

| Step | 작업 | 산출물 |
|------|------|--------|
| 1 | IssueStatus Enum | `IssueStatus.java` |
| 2 | User 엔티티 + Repository | `User.java`, `UserRepository.java` |
| 3 | Coupon 엔티티 + Repository | `Coupon.java`, `CouponRepository.java` |
| 4 | CouponIssue 엔티티 + Repository | `CouponIssue.java`, `CouponIssueRepository.java` |
| 5 | 예외 클래스 6종 | `exception/*.java` |
| 6 | 글로벌 예외 핸들러 | `GlobalExceptionHandler.java` |

## 7. TDD Plan

### Task 1: User 엔티티 비즈니스 로직

1. **테스트 작성 (RED)**
    - `depositPoints()` 호출 시 포인트가 누적되는지 검증
    - `usePoints()` 호출 시 포인트가 차감되는지 검증
    - `usePoints()` 잔액 부족 시 `InsufficientPointsException` 발생 검증
2. **최소 구현 (GREEN)**: User 엔티티 + depositPoints/usePoints 메서드 구현
3. **리팩터링**: 필요 시 검증 로직 정리

### Task 2: Coupon 엔티티 비즈니스 로직

1. **테스트 작성 (RED)**
    - `isAvailable()` — 재고 있고 기간 내일 때 true 반환
    - `isAvailable()` — 재고 0이면 false
    - `isAvailable()` — 기간 외이면 false
    - `decreaseQuantity()` — 정상 차감
    - `decreaseQuantity()` — 재고 0이면 `CouponOutOfStockException` 발생
2. **최소 구현 (GREEN)**: Coupon 엔티티 + isAvailable/decreaseQuantity 구현
3. **리팩터링**: 필요 시 시간 비교 로직 정리

### Task 3: CouponIssue 엔티티

1. **테스트 작성 (RED)**: 생성 시 status = ISSUED, issuedAt이 설정되는지 검증
2. **최소 구현 (GREEN)**: CouponIssue 엔티티 + 생성자 구현
3. **리팩터링**: 없음

### Task 4: Repository 레이어

1. **테스트 작성 (RED)**
    - `CouponIssueRepository.existsByUserIdAndCouponId()` — 존재/미존재 케이스
    - UNIQUE 제약 위반 시 예외 발생 검증
2. **최소 구현 (GREEN)**: 3개 Repository 인터페이스 정의
3. **리팩터링**: 없음

### Task 5: 예외 클래스 + 글로벌 예외 핸들러

1. **테스트 작성 (RED)**: 각 예외 발생 시 올바른 HTTP Status와 메시지가 반환되는지 검증 (MockMvc)
2. **최소 구현 (GREEN)**: 예외 6종 + GlobalExceptionHandler 구현
3. **리팩터링**: 공통 부모 예외 추출 여부 판단

## 8. Verification Criteria

- [ ] `./gradlew :coupon-api:test` 전체 통과
- [ ] 패키지 구조가 5번 섹션과 일치
- [ ] 엔티티 필드가 `docs/design/entity/v1.0.md`와 일치
- [ ] 예외 핸들러가 `docs/spec/v1.0.md` 에러 케이스 테이블과 일치
- [ ] H2 서버 기동 후 애플리케이션 정상 시작 확인 (ddl-auto: create-drop으로 테이블 생성)

## 9. Design Decisions

| 결정 | 근거 |
|------|------|
| BaseEntity 미사용 | 학습 프로젝트로 단순성 우선. createdAt/updatedAt 불필요 |
| CouponIssue에 FK 연관관계 매핑 대신 Long userId/couponId 사용 | INITIAL_CODE.md 참고 코드와 일치. 단순 ID 참조로 복잡도 최소화 |
| UNIQUE 제약을 엔티티에 선언 | DB 레벨에서 중복 발급을 원천 차단. 애플리케이션 검증(existsBy)은 사전 체크용 |
| 예외 클래스를 개별 정의 (공통 부모 없이) | Phase 1에서는 단순하게 시작. 필요 시 리팩터링에서 추출 |
| 에러 응답에 `{ status, message }` 형태 사용 | 간결한 에러 응답. 별도 공통 래핑(RsData 등) 없이 최소 구조 |
