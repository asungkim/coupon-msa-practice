# 선착순 쿠폰 발급 시스템

---

## 1. 시스템 개요

선착순 쿠폰 발급 서비스. 유저가 포인트를 차감하여 쿠폰을 발급받고, 발급 완료 시 외부 알림 서버로 푸시 알림을 보냅니다.

쿠폰 오픈 이벤트가 시작되면 짧은 시간에 트래픽이 몰리는 특성이 있습니다. 운영 중 생기는 다양한 문제들을 고려하여 안정적인 서비스를 구현하고 점진적으로 개선해나갑니다.

---

## 2. 인프라 구성

| 항목 | 설정 |
|---|---|
| 언어/프레임워크 | Java 21 / Spring Boot 3.x |
| DB | H2 (tcp 모드, 모든 서버가 공유) |
| 빌드 | Gradle 멀티모듈 |
| 알림 서버 | 별도 mock 서버 (notification-mock 모듈) |
| 쿠폰 서버 | 2대 운영 (같은 jar, 다른 포트) |
| HikariCP 풀 사이즈 | 10 (수정 금지) |

### 서버 구동 방식

`build.gradle`에 `BootRun` task를 여러 개 등록해서, **같은 애플리케이션을 다른 포트로** 동시에 띄웁니다. IntelliJ Gradle 패널에서 task를 더블클릭하면 각각 실행됩니다.

```groovy
// 쿠폰 서버 1대 (포트 8080)
task couponApi1(type: BootRun) {
    mainClass = 'com.example.coupon.CouponApiApplication'
    args = ['--server.port=8080']
}

// 쿠폰 서버 2대 (포트 8081, 같은 코드 같은 yml)
task couponApi2(type: BootRun) {
    mainClass = 'com.example.coupon.CouponApiApplication'
    args = ['--server.port=8081']
}

// 알림 mock 서버 (포트 8090)
task notificationMock(type: BootRun) {
    sourceSet = project(':notification-mock').sourceSets.main
    mainClass = 'com.example.notification.NotificationMockApplication'
    args = ['--server.port=8090']
}

// H2 tcp 서버
task h2Server(type: JavaExec) {
    classpath = configurations.runtimeClasspath
    mainClass = 'org.h2.tools.Server'
    args = ['-tcp', '-tcpPort', '9092', '-tcpAllowOthers',
            '-web', '-webPort', '9093', '-ifNotExists']
}
```

### 구동 순서

1. `h2Server` 먼저 (DB tcp 서버)
2. `notificationMock` (알림 서버)
3. `couponApi1`, `couponApi2` (쿠폰 서버 2대 동시 구동)

### 두 대 띄우는 의미

- 두 쿠폰 서버는 같은 H2(9092)를 공유함
- LoadTest는 한 서버(예: 8080)만 때리지만, 두 대가 떠 있어야 운영 환경 가정에 맞음
- JVM 메모리 락(synchronized 등)으로는 분산 환경에서 쿠폰 수량 정합성을 보장할 수 없음을 의식해야 함
- 해법은 DB 레벨 처리(비관적 락, 원자적 UPDATE)로 가야 두 서버 환경에서도 안전

---

## 3. 도메인 구성

### 엔티티

- **User** — 유저 정보 (이메일, 이름, 포인트)
- **Coupon** — 쿠폰 마스터 (이름, 설명, pointCost, totalQuantity, remainingQuantity, startDate, endDate)
- **CouponIssue** — 쿠폰 발급 이력 (User-Coupon 매핑 + 상태)

## 4. 제약 사항

- HikariCP 설정 변경 금지 (풀 사이즈 10 고정)
- DB는 H2만 사용
- 쿠폰 서버는 2대 구성 유지
- 컨트롤러 URL, HTTP 메서드, 요청/응답 필드 변경 금지
