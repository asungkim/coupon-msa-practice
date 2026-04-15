package com.example.coupon.domain.coupon.client;

public interface PushNotificationClient {

    void sendCouponIssuedNotification(Long userId, String couponName, Long couponId);
}
