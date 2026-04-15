package com.example.coupon.domain.coupon.controller;

import com.example.coupon.domain.coupon.dto.CouponResponse;
import com.example.coupon.domain.coupon.dto.CreateCouponRequest;
import com.example.coupon.domain.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public CouponResponse createCoupon(@RequestBody CreateCouponRequest request) {
        return CouponResponse.from(couponService.createCoupon(
                request.name(), request.description(), request.pointCost(),
                request.totalQuantity(), request.startDate(), request.endDate()));
    }
}
