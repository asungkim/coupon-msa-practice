package com.example.coupon.domain.coupon.exception;

public class CouponNotAvailableException extends RuntimeException {

    public CouponNotAvailableException(Long couponId, String reason) {
        super("Coupon is not available or expired: couponId=" + couponId + ", reason=" + reason);
    }
}
