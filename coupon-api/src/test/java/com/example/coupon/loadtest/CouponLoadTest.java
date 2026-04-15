package com.example.coupon.loadtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 서버 기동 후 수동 실행하는 부하 테스트.
 * h2Server → notificationMock → couponApi1 순서로 기동 후 실행.
 */
@EnabledIfEnvironmentVariable(named = "LOAD_TEST", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CouponLoadTest {

    private final String baseUrl = System.getenv("BASE_URL") != null
            ? System.getenv("BASE_URL")
            : "http://localhost:8080";

    private final WebClient client = WebClient.create(baseUrl);

    private final int COUPON_QUANTITY = 200;
    private final int CONCURRENT_USERS = 300;

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

    private ClientResponse get(String path) {
        return client
                .get()
                .uri(path)
                .exchange()
                .block();
    }

    private void depositPoints(Long userId, Long amount) {
        post("/api/users/" + userId + "/points", Map.of("amount", amount));
    }

    @Test
    @DisplayName("부하 테스트 — 300명 동시 발급 (쿠폰 200개) 정합성 검증")
    void loadTest_concurrentIssueCoupon() throws Exception {
        Long runId = System.currentTimeMillis();

        // 1. 유저 300명 생성 + 포인트 충전
        System.out.println(">>> 유저 " + CONCURRENT_USERS + "명 생성 중...");
        List<Long> userIds = new ArrayList<>();
        for (int i = 1; i <= CONCURRENT_USERS; i++) {
            var body = jsonMap(
                    post("/api/users", Map.of(
                            "email", "Load" + runId + "-" + i + "@test.com",
                            "name", "LoadUser" + i
                    ))
            );
            Long userId = ((Number) body.get("id")).longValue();
            depositPoints(userId, 10000L);
            userIds.add(userId);
        }

        // 2. 쿠폰 생성 (수량 200)
        var couponBody = jsonMap(
                post("/api/coupons", Map.of(
                        "name", "대박쿠폰_" + runId,
                        "description", "부하테스트용 쿠폰",
                        "pointCost", 100,
                        "totalQuantity", COUPON_QUANTITY,
                        "startDate", LocalDateTime.now().minusDays(1).toString(),
                        "endDate", LocalDateTime.now().plusDays(30).toString()
                ))
        );
        Long couponId = ((Number) couponBody.get("id")).longValue();

        // 3. 동시 요청 준비
        var executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        var latch = new CountDownLatch(1);
        var successCount = new AtomicInteger(0);
        var failCount = new AtomicInteger(0);
        var errorCount = new AtomicInteger(0);
        Long startTime = System.currentTimeMillis();

        // 4. 300명 동시 발급 요청
        List<Future<HttpStatus>> futures = new ArrayList<>();
        for (Long userId : userIds) {
            futures.add(
                    executor.submit(() -> {
                        latch.await();
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

        // 6. 타임아웃 10초
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
                    if (status == HttpStatus.OK) {
                        successCount.incrementAndGet();
                    } else if (status.is4xxClientError()) {
                        failCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
            }
        }

        // 8. 결과 출력
        System.out.println();
        System.out.println("=".repeat(55));
        System.out.println("  부하테스트 결과 (Phase 1 — 동시성 미해결)");
        System.out.println("=".repeat(55));
        System.out.println("대상 서버: " + baseUrl);
        System.out.println("쿠폰 수량: " + COUPON_QUANTITY);
        System.out.println("동시 요청 수: " + CONCURRENT_USERS);
        System.out.println("-".repeat(55));
        System.out.printf("HTTP 200 성공: %d%n", successCount.get());
        System.out.printf("HTTP 4xx 실패: %d%n", failCount.get());
        System.out.printf("에러/타임아웃: %d%n", errorCount.get());
        System.out.printf("총 소요시간: %.1f초%n", elapsedSeconds);
        System.out.println("-".repeat(55));

        // 9. 정합성 검증
        boolean hasIssue = false;

        if (successCount.get() > COUPON_QUANTITY) {
            System.out.printf("[FAIL] 초과 발급! 성공(%d) > 쿠폰수량(%d)%n",
                    successCount.get(), COUPON_QUANTITY);
            hasIssue = true;
        } else if (successCount.get() == COUPON_QUANTITY) {
            System.out.printf("[PASS] 발급 수량 정확: %d == %d%n",
                    successCount.get(), COUPON_QUANTITY);
        } else {
            System.out.printf("[WARN] 발급 부족: 성공(%d) < 쿠폰수량(%d) — 커넥션 고갈 등으로 일부 실패%n",
                    successCount.get(), COUPON_QUANTITY);
            hasIssue = true;
        }

        if (elapsedSeconds > 10.0) {
            System.out.printf("[FAIL] 소요시간 초과: %.1f초 > 10초 — 커넥션 고갈 의심%n", elapsedSeconds);
            hasIssue = true;
        }

        System.out.println("=".repeat(55));
        if (hasIssue) {
            System.out.println(">>> Phase 1 문제 확인됨 — Phase 2~3에서 해결 필요");
        } else {
            System.out.println(">>> 정합성 문제 없음");
        }
        System.out.println("=".repeat(55));
    }
}
