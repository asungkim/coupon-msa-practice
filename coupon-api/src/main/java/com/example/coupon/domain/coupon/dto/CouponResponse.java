package com.example.coupon.domain.coupon.dto;

import com.example.coupon.domain.coupon.entity.Coupon;

import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String name,
        String description,
        Integer pointCost,
        Integer totalQuantity,
        Integer remainingQuantity,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDescription(),
                coupon.getPointCost(),
                coupon.getTotalQuantity(),
                coupon.getRemainingQuantity(),
                coupon.getStartDate(),
                coupon.getEndDate()
        );
    }
}
