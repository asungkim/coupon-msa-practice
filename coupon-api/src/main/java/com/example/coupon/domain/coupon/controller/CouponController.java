package com.example.coupon.domain.coupon.controller;

import com.example.coupon.domain.coupon.dto.CouponIssueResponse;
import com.example.coupon.domain.coupon.dto.CouponResponse;
import com.example.coupon.domain.coupon.dto.CreateCouponRequest;
import com.example.coupon.domain.coupon.dto.IssueCouponRequest;
import com.example.coupon.domain.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{couponId}")
    public CouponResponse getCoupon(@PathVariable Long couponId) {
        return CouponResponse.from(couponService.getCoupon(couponId));
    }

    @GetMapping("/{couponId}/issues/count")
    public long getIssueCount(@PathVariable Long couponId) {
        return couponService.getIssueCount(couponId);
    }

    @PostMapping("/{couponId}/issue")
    public CouponIssueResponse issueCoupon(@PathVariable Long couponId,
                                           @RequestBody IssueCouponRequest request) {
        return couponService.issueCoupon(request.userId(), couponId);
    }
}
