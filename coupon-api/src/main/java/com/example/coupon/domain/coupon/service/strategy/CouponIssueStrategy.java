package com.example.coupon.domain.coupon.service.strategy;

import com.example.coupon.domain.coupon.entity.Coupon;

public interface CouponIssueStrategy {

    /**
     * 쿠폰 재고를 차감한다.
     * @return 차감된 쿠폰 (발급 가능 검증 포함)
     */
    Coupon decreaseQuantity(Long couponId);
}
