# Feature: Spring Cloud Gateway 도입 — 2대 서버 로드밸런싱

## 1. Problem Definition

- 쿠폰 서버를 2대(8080, 8081) 띄울 수 있는 구조이지만, 요청 분산을 하지 않고 있다
- 부하 테스트도 8080 한 대만 때리고 있어 실제 분산 환경 검증이 안 됨
- 2대 서버가 같은 DB를 동시에 때릴 때 동시성 제어가 제대로 동작하는지 확인해야 함

## 2. 배경 지식

### API Gateway 패턴

- 모든 클라이언트 요청의 **단일 진입점** 역할
- 라우팅, 로드밸런싱, 공통 관심사(인증, 로깅 등)를 한 곳에서 처리
- 클라이언트는 내부 서비스 구조를 몰라도 됨

### Spring Cloud Gateway vs AWS ALB

| | AWS ALB | Spring Cloud Gateway |
|---|---------|---------------------|
| 본질 | 인프라 레벨 로드밸런서 | 애플리케이션 레벨 게이트웨이 |
| 동작 | AWS 네트워크에서 처리 | JVM(WebFlux/Netty) 프로세스 |
| 비즈니스 로직 | 불가 (단순 라우팅만) | 가능 (인증, 필터, 서킷브레이커 등) |
| 관리 | AWS 관리형 | 직접 운영/배포 |

실무에서는 ALB(인프라) + Gateway(애플리케이션) 둘 다 사용하는 구성이 일반적.
우리 프로젝트에서는 학습 목적으로 Spring Cloud Gateway를 직접 구축한다.

### Spring Cloud Gateway 핵심 개념

- **Route**: 조건(Predicate) + 필터(Filter) + 목적지(URI)
- **Predicate**: 요청 매칭 조건 (Path, Method, Header 등)
- **Filter**: Pre/Post 요청 가공 (헤더 추가, 로깅, 서킷브레이커 등)
- **LoadBalancer**: `lb://서비스명`으로 라운드로빈 분산

## 3. 아키텍처

```
                                    ┌→ coupon-api-1 (8080) ──→ H2 DB (9092)
유저/테스트 → Gateway (9000) ──────┤
                                    └→ coupon-api-2 (8081) ──┘

notification-mock (8090) ← 쿠폰 서버들이 직접 호출
```

- Gateway는 쿠폰 API 요청만 라우팅 (알림 서버는 쿠폰 서버가 직접 호출)
- 정적 인스턴스 목록으로 8080, 8081 등록 (Eureka 없이)
- 라운드로빈으로 분산

## 4. Prerequisites

- ✅ 쿠폰 서버 2대 구동 구조 (bootRun, bootRun2)
- ✅ 동시성 해결 (비관적 락 / 원자적 UPDATE)
- ✅ 부하 테스트 기반 (CouponLoadTest)

## 5. 구현 계획

### 5.1 Gateway 모듈 추가

- 새 Gradle 모듈: `gateway`
- Spring Cloud Gateway + Spring Cloud LoadBalancer 의존성
- 포트: 9000

### 5.2 라우팅 설정

```yaml
spring:
  application:
    name: gateway
  cloud:
    discovery:
      client:
        simple:
          instances:
            coupon-api:
              - uri: http://localhost:8080
              - uri: http://localhost:8081
    gateway:
      routes:
        - id: coupon-service
          uri: lb://coupon-api
          predicates:
            - Path=/api/**
```

- `/api/**` 요청을 coupon-api 인스턴스(8080, 8081)로 라운드로빈 분산
- Eureka 없이 `simple` 디스커버리로 정적 인스턴스 등록

### 5.3 서버 구동 순서

1. h2Server (9092)
2. notification-mock (8090)
3. coupon-api-1 (8080)
4. coupon-api-2 (8081)
5. gateway (9000)

### 5.4 부하 테스트

- 기존 `CouponLoadTest`는 **단일 인스턴스 전용**으로 유지 (8080 직접 호출)
- 새로 `GatewayLoadTest`를 작성 — **Gateway(9000) 경유 2대 분산 전용**
- 동일 조건(300명, 1000명/200쿠폰)으로 단일 vs 분산 성능 비교

### 5.5 성능 비교 관점

2대 서버로 분산하면 처리량이 향상되는지 확인:
- 단일(8080 직접): HikariCP 10개로 처리
- 분산(Gateway → 8080 + 8081): HikariCP 10개 × 2대 = 커넥션 20개 효과
- 같은 DB row에 대한 락 경합은 동일하므로, 순수 처리량 개선 폭을 측정

## 6. 패키지 구조

```
coupon-msa/
├── gateway/                          (신규 모듈)
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../GatewayApplication.java
│       └── resources/application.yml
├── coupon-api/                       (기존)
│   └── src/test/
│       └── loadtest/
│           ├── CouponLoadTest.java           (기존 — 단일 인스턴스 전용)
│           └── GatewayLoadTest.java          (신규 — Gateway 분산 전용)
├── notification-mock-api/            (기존)
└── settings.gradle                   (gateway 모듈 추가)
```

## 7. TDD Plan

### Task 1: Gateway 모듈 셋업

1. `settings.gradle`에 `gateway` 모듈 추가
2. `gateway/build.gradle` — Spring Cloud Gateway + LoadBalancer 의존성
3. `GatewayApplication.java` — Spring Boot main
4. `application.yml` — 라우팅 설정 + 정적 인스턴스 목록
5. 서버 기동 확인 (Gateway → coupon-api 라우팅 동작)

### Task 2: GatewayLoadTest 작성

1. CouponLoadTest를 기반으로 GatewayLoadTest 작성
2. BASE_URL 기본값을 `http://localhost:9000`으로 설정
3. 결과 파일에 "Gateway 분산" 라벨로 기록

### Task 3: 2대 서버 분산 부하 테스트 + 비교

1. h2 + notification-mock + coupon-api-1 + coupon-api-2 + gateway 전부 기동
2. GatewayLoadTest 실행 (300명, 1000명)
3. 기존 CouponLoadTest 결과(단일)와 비교:
   - 정합성 동일한지
   - 소요시간 개선 여부
   - 2대 서버 간 분산 확인 (서버 로그)

## 8. Verification Criteria

- [ ] Gateway 모듈이 정상 기동되고 `/api/**` 요청이 쿠폰 서버로 라우팅됨
- [ ] 라운드로빈으로 8080/8081에 분산되는지 로그로 확인
- [ ] 2대 서버 분산 부하 테스트에서 정합성 PASS
- [ ] 단일 vs 분산 성능 비교 결과가 `docs/history/load_test_results.md`에 기록

## 9. Design Decisions

| 결정 | 근거 |
|------|------|
| Eureka 없이 정적 인스턴스 목록 사용 | 외부 인프라 최소화. 학습 목적에 서비스 디스커버리까지는 과도 |
| Gateway 포트 9000 | 기존 서비스(8080, 8081, 8090, 9092)와 충돌 없는 포트 |
| 알림 서버는 Gateway를 거치지 않음 | 알림은 쿠폰 서버가 내부적으로 호출하는 것. 외부 요청이 아님 |
| Gateway에 필터/서킷브레이커 미적용 (Phase 2) | 로드밸런싱 검증에 집중. 필터 등은 Phase 3에서 추가 가능 |
| CouponLoadTest(단일)와 GatewayLoadTest(분산) 분리 | 각각 독립적으로 실행 가능. 비교 시 동일 조건 보장 |
