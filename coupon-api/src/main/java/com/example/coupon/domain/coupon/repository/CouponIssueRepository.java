package com.example.coupon.domain.coupon.repository;

import com.example.coupon.domain.coupon.entity.CouponIssue;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    long countByCouponId(Long couponId);
}
