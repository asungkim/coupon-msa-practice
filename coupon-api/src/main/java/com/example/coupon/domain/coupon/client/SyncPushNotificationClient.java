package com.example.coupon.domain.coupon.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.client.mode", havingValue = "sync", matchIfMissing = true)
public class SyncPushNotificationClient implements PushNotificationClient {

    private final RestClient restClient;

    record NotificationRequest(Long userId, String title, String message, Long couponId) {}
    record NotificationResponse(boolean success, String messageId) {}

    public SyncPushNotificationClient(
            @Value("${notification.service.url}") String notificationUrl,
            @Value("${notification.service.timeout-seconds}") int timeoutSeconds
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(notificationUrl)
                .build();
    }

    @Override
    public void sendCouponIssuedNotification(Long userId, String couponName, Long couponId) {
        log.info("Sending push notification (sync) to user {} for coupon {}", userId, couponId);

        var request = new NotificationRequest(
                userId,
                "쿠폰 발급 완료!",
                "[" + couponName + "] 쿠폰이 발급되었습니다.",
                couponId
        );

        var response = restClient
                .post()
                .uri("/api/push/send")
                .body(request)
                .retrieve()
                .body(NotificationResponse.class);

        log.info("Notification sent successfully (sync). MessageId: {}",
                response != null ? response.messageId() : null);
    }
}
