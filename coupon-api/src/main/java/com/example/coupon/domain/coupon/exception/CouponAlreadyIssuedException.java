package com.example.coupon.domain.coupon.exception;

public class CouponAlreadyIssuedException extends RuntimeException {

    public CouponAlreadyIssuedException(Long userId, Long couponId) {
        super("Coupon already issued to this user: userId=" + userId + ", couponId=" + couponId);
    }
}
