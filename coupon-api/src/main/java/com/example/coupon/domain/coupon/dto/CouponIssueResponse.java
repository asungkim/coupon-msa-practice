package com.example.coupon.domain.coupon.dto;

import com.example.coupon.domain.coupon.entity.CouponIssue;

import java.time.LocalDateTime;

public record CouponIssueResponse(
        Long id,
        Long userId,
        Long couponId,
        String status,
        LocalDateTime issuedAt
) {
    public static CouponIssueResponse from(CouponIssue issue) {
        return new CouponIssueResponse(
                issue.getId(),
                issue.getUserId(),
                issue.getCouponId(),
                issue.getStatus().name(),
                issue.getIssuedAt()
        );
    }
}
