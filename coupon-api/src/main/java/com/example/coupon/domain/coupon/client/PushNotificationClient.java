package com.example.coupon.domain.coupon.client;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

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

    public void sendCouponIssuedNotification(Long userId, String couponName, Long couponId) {
        log.info("Sending push notification to user {} for coupon {}", userId, couponId);

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
