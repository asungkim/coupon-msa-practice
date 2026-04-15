package com.example.coupon.domain.coupon.event;

import com.example.coupon.domain.coupon.client.PushNotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssuedEventListener {

    private final PushNotificationClient pushNotificationClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponIssued(CouponIssuedEvent event) {
        log.info("Handling CouponIssuedEvent: userId={}, couponId={}", event.userId(), event.couponId());
        try {
            pushNotificationClient.sendCouponIssuedNotification(
                    event.userId(), event.couponName(), event.couponId());
        } catch (Exception e) {
            log.error("Failed to send notification for userId={}, couponId={}",
                    event.userId(), event.couponId(), e);
        }
    }
}
