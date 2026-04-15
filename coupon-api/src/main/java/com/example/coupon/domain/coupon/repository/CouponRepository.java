package com.example.coupon.domain.coupon.repository;

import com.example.coupon.domain.coupon.entity.Coupon;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
