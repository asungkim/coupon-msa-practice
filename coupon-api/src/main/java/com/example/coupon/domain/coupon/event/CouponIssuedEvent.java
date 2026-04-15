package com.example.coupon.domain.coupon.event;

public record CouponIssuedEvent(Long userId, String couponName, Long couponId) {
}
