# 참고용 코드

이 문서는 참고용으로만 사용한다. 바로 이코드로 만들지 않는다.

---

## 1. Coupon 엔티티

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Integer pointCost;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public Coupon(String name, String description, Integer pointCost,
                  Integer totalQuantity, LocalDateTime startDate, LocalDateTime endDate) {
        this.name = name;
        this.description = description;
        this.pointCost = pointCost;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean isAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return remainingQuantity > 0
                && now.isAfter(startDate)
                && now.isBefore(endDate);
    }

    public void decreaseQuantity() {
        if (remainingQuantity <= 0) {
            throw new CouponOutOfStockException(id);
        }
        remainingQuantity--;
    }
}
```

---

## 2. CouponIssue 엔티티

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long couponId;

    @Enumerated(EnumType.STRING)
    private IssueStatus status;

    private LocalDateTime issuedAt;

    public CouponIssue(Long userId, Long couponId, IssueStatus status) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = status;
        this.issuedAt = LocalDateTime.now();
    }
}

public enum IssueStatus {
    ISSUED, USED, EXPIRED
}
```

---

## 3. CouponRepository

```java
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    // 별도 메서드 없음 (기본 CRUD만)
}
```

---

## 4. CouponIssueRepository

```java
public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
```

---

## 5. CouponService (개선 전 — 문제 있는 상태)

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final PushNotificationClient pushNotificationClient;

    @Transactional
    public CouponIssue issueCoupon(Long userId, Long couponId) {
        log.info("User {} attempting to issue coupon {}", userId, couponId);

        if (couponIssueRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new CouponAlreadyIssuedException(userId, couponId);
        }

        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        var coupon = couponRepository.findById(couponId).orElse(null);
        if (coupon == null) {
            throw new CouponNotFoundException(couponId);
        }

        if (!coupon.isAvailable()) {
            throw new CouponNotAvailableException(
                    couponId, "Coupon is not available or expired");
        }

        pointService.decreasePoints(userId, coupon.getPointCost());

        coupon.decreaseQuantity();

        var couponIssue = new CouponIssue(userId, couponId, IssueStatus.ISSUED);
        var savedIssue = couponIssueRepository.save(couponIssue);

        pushNotificationClient.sendCouponIssuedNotification(
                userId,
                coupon.getName(),
                coupon.getId()
        );

        log.info("Coupon {} issued to user {} successfully", couponId, userId);

        return savedIssue;
    }
}
```

---

## 6. PointService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;

    @Transactional
    public void decreasePoints(Long userId, Long amount) {
        log.info("Deducting {} points from user {}", amount, userId);
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        user.usePoints(amount);
    }
}
```

---

## 7. PushNotificationClient

```java
@Slf4j
@Component
public class PushNotificationClient {

    private final WebClient webClient;

    public PushNotificationClient(
            @Value("${notification.service.url}") String notificationUrl,
            @Value("${notification.service.timeout-seconds}") int timeoutSeconds
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(notificationUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                        timeoutSeconds * 1000)
                                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                ))
                .build();
    }

    record NotificationRequest(
            Long userId,
            String title,
            String message,
            Long couponId
    ) {}

    record NotificationResponse(boolean success, String messageId) {}

    public void sendCouponIssuedNotification(
            Long userId, String couponName, Long couponId
    ) {
        log.info("Sending push notification to user {} for coupon {}",
                userId, couponId);

        var request = new NotificationRequest(
                userId,
                "쿠폰 발급 완료!",
                "[" + couponName + "] 쿠폰이 발급되었습니다.",
                couponId
        );

        var response = webClient
                .post()
                .uri("/api/push/send")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NotificationResponse.class)
                .block();

        log.info("Notification sent successfully. MessageId: {}",
                response != null ? response.messageId() : null);
    }
}
```

---

## 8. application.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost:9092/mem:coupon
    driver-class-name: org.h2.Driver
    username: sa
    password:
    hikari:
      maximum-pool-size: 10  # 변경 금지
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

notification:
  service:
    url: http://localhost:8090
    timeout-seconds: 5
```

---

## 9. CouponLoadTest

```java
// 사전 조건: 쿠폰 서비스가 정상적으로 기동된 상태에서 실행해야 합니다.
// 서비스가 기동되지 않은 상태에서 실행하면 Connection refused 오류가 발생합니다.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CouponLoadTest {

    private final String baseUrl = System.getenv("BASE_URL") != null
            ? System.getenv("BASE_URL")
            : "http://localhost:8080";

    private final WebClient client = WebClient.create(baseUrl);

    private ClientResponse post(String path, Object body) {
        return client
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .block();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonMap(ClientResponse resp) {
        return (Map<String, Object>) resp.bodyToMono(Map.class).block();
    }

    private void depositPoints(Long userId, Long amount) {
        post("/api/users/" + userId + "/points", Map.of("amount", amount));
    }

    // ==========================================
    // 부하 테스트
    // ==========================================
    @Test
    @DisplayName("대박쿠폰 이벤트 오픈 - 동시 발급 부하 테스트")
    void loadTest_concurrentIssueCoupon() {
        Long runId = System.currentTimeMillis();
        int concurrency = 200;

        // 1. 유저 200명 생성 + 포인트 충전
        List<Long> userIds = new ArrayList<>();
        for (int i = 1; i <= concurrency; i++) {
            var body = jsonMap(
                    post(
                            "/api/users",
                            Map.of(
                                    "email", "Load" + runId + "-" + i + "@test.com",
                                    "name", "LoadUser" + i
                            )
                    )
            );
            Long userId = ((Number) body.get("id")).longValue();
            depositPoints(userId, 10000L);
            userIds.add(userId);
        }

        // 2. 쿠폰 생성
        var couponBody = jsonMap(
                post(
                        "/api/coupons",
                        Map.of(
                                "name", "대박쿠폰_" + runId,
                                "description", "이벤트 오픈 기념 대박쿠폰",
                                "pointCost", 100,
                                "totalQuantity", concurrency,
                                "startDate", LocalDateTime.now().minusDays(1).toString(),
                                "endDate", LocalDateTime.now().plusDays(30).toString()
                        )
                )
        );
        Long couponId = ((Number) couponBody.get("id")).longValue();

        // 3. 동시 요청 준비
        var executor = Executors.newFixedThreadPool(concurrency);
        var latch = new CountDownLatch(1);
        var successCount = new AtomicInteger(0);
        Long startTime = System.currentTimeMillis();

        // 4. 각 유저가 동시에 쿠폰 발급 요청
        List<Future<HttpStatus>> futures = new ArrayList<>();
        for (Long userId : userIds) {
            futures.add(
                    executor.submit(() -> {
                        latch.await();  // 모든 스레드가 여기서 대기
                        try {
                            var resp = post(
                                    "/api/coupons/" + couponId + "/issue",
                                    Map.of("userId", userId)
                            );
                            return (HttpStatus) resp.statusCode();
                        } catch (Exception e) {
                            return HttpStatus.INTERNAL_SERVER_ERROR;
                        }
                    })
            );
        }

        // 5. 동시 출발
        latch.countDown();

        // 6. 전체 테스트 타임아웃: 10초 후 일괄 종료
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            executor.shutdownNow();
        }

        // 7. 결과 집계
        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        for (var future : futures) {
            try {
                if (future.isDone()) {
                    var status = future.get();
                    if (status == HttpStatus.OK) successCount.incrementAndGet();
                }
            } catch (Exception e) {
                // cancelled or execution failure
            }
        }

        // 8. 결과 출력
        System.out.println("========== 대박쿠폰 이벤트 부하테스트 결과 ==========");
        System.out.println("대상 서버: " + baseUrl);
        System.out.println("동시 요청 수: " + concurrency);
        System.out.printf("성공: %d / %d%n", successCount.get(), concurrency);
        System.out.printf("총 소요시간: %.1f초%n", elapsedSeconds);
        System.out.println("===================================================");
    }
}
```

---

## 핵심 의도

이 코드는 **의도적으로 문제를 가지고 있습니다.**

1. `pushNotificationClient.sendCouponIssuedNotification()` — 트랜잭션 안에서 동기 호출 (CS 1번 원인)
2. `coupon.decreaseQuantity()` — Java에서 단순 `--` 처리, 동시성 문제 발생 (LoadTest 실패 원인)
3. 락 없음 — 200명 동시 요청 시 lost update + 커넥션 고갈

이 세 가지를 진단하고 해결하는 것이 과제입니다.
