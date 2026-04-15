package com.example.coupon.domain.coupon.exception;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(Long couponId) {
        super("Coupon not found: " + couponId);
    }
}
