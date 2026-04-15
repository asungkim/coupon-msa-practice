package com.example.coupon.domain.coupon.exception;

public class CouponOutOfStockException extends RuntimeException {

    public CouponOutOfStockException(Long couponId) {
        super("Coupon out of stock: " + couponId);
    }
}
