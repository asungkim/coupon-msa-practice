package com.example.notification.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RestController
public class NotificationController {

    @PostMapping("/api/push/send")
    public Map<String, Object> sendPush(@RequestBody Map<String, Object> request) throws InterruptedException {
        log.info("Push notification received: userId={}, title={}, couponId={}",
                request.get("userId"), request.get("title"), request.get("couponId"));

        // 실제 외부 API 호출 지연 시뮬레이션 (2~3초)
        Thread.sleep(ThreadLocalRandom.current().nextLong(2000, 3001));

        String messageId = UUID.randomUUID().toString();
        log.info("Push notification sent. messageId={}", messageId);

        return Map.of(
                "success", true,
                "messageId", messageId);
    }
}
