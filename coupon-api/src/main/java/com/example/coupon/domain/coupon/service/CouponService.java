package com.example.coupon.domain.coupon.service;

import com.example.coupon.domain.coupon.entity.Coupon;
import com.example.coupon.domain.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public Coupon createCoupon(String name, String description, Integer pointCost,
                               Integer totalQuantity, LocalDateTime startDate, LocalDateTime endDate) {
        Coupon coupon = new Coupon(name, description, pointCost, totalQuantity, startDate, endDate);
        return couponRepository.save(coupon);
    }
}
