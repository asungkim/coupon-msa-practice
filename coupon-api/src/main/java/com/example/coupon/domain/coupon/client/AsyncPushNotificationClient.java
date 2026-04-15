package com.example.coupon.domain.coupon.client;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.client.mode", havingValue = "async")
public class AsyncPushNotificationClient implements PushNotificationClient {

    private final WebClient webClient;

    record NotificationRequest(Long userId, String title, String message, Long couponId) {}
    record NotificationResponse(boolean success, String messageId) {}

    public AsyncPushNotificationClient(
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

    @Override
    public void sendCouponIssuedNotification(Long userId, String couponName, Long couponId) {
        log.info("Sending push notification (async) to user {} for coupon {}", userId, couponId);

        var request = new NotificationRequest(
                userId,
                "쿠폰 발급 완료!",
                "[" + couponName + "] 쿠폰이 발급되었습니다.",
                couponId
        );

        webClient
                .post()
                .uri("/api/push/send")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NotificationResponse.class)
                .subscribe(
                        response -> log.info("Notification sent successfully (async). MessageId: {}",
                                response != null ? response.messageId() : null),
                        error -> log.error("Notification failed (async) for user {} coupon {}",
                                userId, couponId, error)
                );
    }
}
