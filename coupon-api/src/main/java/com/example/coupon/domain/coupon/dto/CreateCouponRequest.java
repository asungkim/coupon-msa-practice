package com.example.coupon.domain.coupon.dto;

import java.time.LocalDateTime;

public record CreateCouponRequest(
        String name,
        String description,
        Integer pointCost,
        Integer totalQuantity,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
}
