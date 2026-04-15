package com.example.coupon.loadtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gateway(9000) 경유 2대 서버 분산 부하 테스트.
 * h2Server → notificationMock → couponApi1 → couponApi2 → gateway 순서로 기동 후 실행.
 *
 * 실행: GATEWAY_LOAD_TEST=true ./gradlew :coupon-api:test --tests "*.GatewayLoadTest" --rerun
 */
@EnabledIfEnvironmentVariable(named = "GATEWAY_LOAD_TEST", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GatewayLoadTest {

    private final String baseUrl = System.getenv("BASE_URL") != null
            ? System.getenv("BASE_URL")
            : "http://localhost:9000";

    private final String testLabel = System.getenv("LOAD_TEST_LABEL") != null
            ? System.getenv("LOAD_TEST_LABEL")
            : "Gateway 분산";

    private final WebClient client = WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                    reactor.netty.http.client.HttpClient.create()
                            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                            .responseTimeout(java.time.Duration.ofSeconds(30))
            ))
            .build();

    private final int COUPON_QUANTITY = parseEnvOrDefault("LOAD_TEST_QUANTITY", 200);
    private final int CONCURRENT_USERS = parseEnvOrDefault("LOAD_TEST_USERS", 300);

    private static final Path RESULT_FILE = Path.of(System.getProperty("user.dir"))
            .getParent()
            .resolve("docs/history/load_test_results.md");

    private static int parseEnvOrDefault(String key, int defaultValue) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) return defaultValue;
        return Integer.parseInt(val);
    }

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path) {
        return (Map<String, Object>) client
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private Long getLong(String path) {
        return client
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono(Long.class)
                .block();
    }

    @Test
    @DisplayName("Gateway 분산 부하 테스트 — 2대 서버 로드밸런싱 정합성 검증")
    void loadTest_gateway_distributed() throws Exception {
        Long runId = System.currentTimeMillis();

        System.out.println(">>> [Gateway 분산] 유저 " + CONCURRENT_USERS + "명 생성 중...");
        List<Long> userIds = new ArrayList<>();
        for (int i = 1; i <= CONCURRENT_USERS; i++) {
            var body = jsonMap(
                    post("/api/users", Map.of(
                            "email", "GW" + runId + "-" + i + "@test.com",
                            "name", "GWUser" + i
                    ))
            );
            Long userId = ((Number) body.get("id")).longValue();
            depositPoints(userId, 10000L);
            userIds.add(userId);
        }

        var couponBody = jsonMap(
                post("/api/coupons", Map.of(
                        "name", "GW쿠폰_" + runId,
                        "description", "Gateway 분산 테스트용",
                        "pointCost", 100,
                        "totalQuantity", COUPON_QUANTITY,
                        "startDate", LocalDateTime.now().minusDays(1).toString(),
                        "endDate", LocalDateTime.now().plusDays(30).toString()
                ))
        );
        Long couponId = ((Number) couponBody.get("id")).longValue();

        var executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        var latch = new CountDownLatch(1);
        var successCount = new AtomicInteger(0);
        var failCount = new AtomicInteger(0);
        var errorCount = new AtomicInteger(0);
        Long startTime = System.currentTimeMillis();

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

        latch.countDown();

        int timeoutSeconds = Math.max(10, CONCURRENT_USERS / 30);
        executor.shutdown();
        try {
            executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            executor.shutdownNow();
        }

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
                } else {
                    errorCount.incrementAndGet();
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
            }
        }

        // DB 정합성 검증
        var couponState = get("/api/coupons/" + couponId);
        int remainingQuantity = ((Number) couponState.get("remainingQuantity")).intValue();
        int totalQuantity = ((Number) couponState.get("totalQuantity")).intValue();
        long issueCount = getLong("/api/coupons/" + couponId + "/issues/count");
        int consumed = totalQuantity - remainingQuantity;
        boolean consistencyMatch = (consumed == issueCount);

        // 콘솔 출력
        System.out.println();
        System.out.println("=".repeat(55));
        System.out.printf("  Gateway 분산 부하테스트 [%s]%n", testLabel);
        System.out.println("=".repeat(55));
        System.out.println("대상: " + baseUrl + " → 8080 + 8081 (라운드로빈)");
        System.out.println("쿠폰 수량: " + COUPON_QUANTITY);
        System.out.println("동시 요청 수: " + CONCURRENT_USERS);
        System.out.println("-".repeat(55));
        System.out.printf("HTTP 200 성공: %d%n", successCount.get());
        System.out.printf("HTTP 4xx 실패: %d%n", failCount.get());
        System.out.printf("에러/타임아웃: %d%n", errorCount.get());
        System.out.printf("총 소요시간: %.1f초%n", elapsedSeconds);
        System.out.println("-".repeat(55));
        System.out.printf("remainingQuantity: %d%n", remainingQuantity);
        System.out.printf("실제 발급 건수(DB): %d%n", issueCount);
        System.out.printf("소비량(total-remaining): %d%n", consumed);
        System.out.printf("정합성(소비량==발급건수): %s%n", consistencyMatch ? "PASS" : "FAIL");
        System.out.println("=".repeat(55));

        saveResult(elapsedSeconds, successCount.get(), failCount.get(), errorCount.get(),
                remainingQuantity, issueCount, consumed, consistencyMatch);
    }

    private void saveResult(double elapsed, int success, int fail, int error,
                            int remaining, long issueCount, int consumed,
                            boolean consistency) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        var sb = new StringBuilder();
        sb.append("\n## [%s] %s\n\n".formatted(timestamp, testLabel));
        sb.append("| 항목 | 값 |\n");
        sb.append("|------|-----|\n");
        sb.append("| 서버 구성 | Gateway(9000) → 8080 + 8081 (2대 분산) |\n");
        sb.append("| 쿠폰 수량 | %d |\n".formatted(COUPON_QUANTITY));
        sb.append("| 동시 요청 수 | %d |\n".formatted(CONCURRENT_USERS));
        sb.append("| HTTP 200 성공 | %d |\n".formatted(success));
        sb.append("| HTTP 4xx 실패 | %d |\n".formatted(fail));
        sb.append("| 에러/타임아웃 | %d |\n".formatted(error));
        sb.append("| 총 소요시간 | %.1f초 |\n".formatted(elapsed));
        sb.append("| remainingQuantity | %d |\n".formatted(remaining));
        sb.append("| 실제 발급 건수(DB) | %d |\n".formatted(issueCount));
        sb.append("| 소비량(total-remaining) | %d |\n".formatted(consumed));
        sb.append("| 정합성(소비량==발급건수) | %s |\n".formatted(consistency ? "PASS" : "FAIL"));

        if (!Files.exists(RESULT_FILE)) {
            Files.createDirectories(RESULT_FILE.getParent());
            Files.writeString(RESULT_FILE, "# 부하 테스트 결과 기록\n\n> 각 단계별 동일 조건으로 실행한 결과를 누적 기록한다.\n");
        }
        Files.writeString(RESULT_FILE, sb.toString(), StandardOpenOption.APPEND);

        System.out.println(">>> 결과 저장: " + RESULT_FILE);
    }
}
