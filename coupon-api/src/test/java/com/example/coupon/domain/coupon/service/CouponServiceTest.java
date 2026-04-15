package com.example.coupon.domain.coupon.service;

import com.example.coupon.domain.coupon.entity.Coupon;
import com.example.coupon.domain.coupon.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CouponServiceTest {

    private CouponRepository couponRepository;
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponRepository = mock(CouponRepository.class);
        couponService = new CouponService(couponRepository);
    }

    @Test
    @DisplayName("쿠폰 생성 시 remainingQuantity == totalQuantity로 저장된다")
    void createCoupon() {
        Coupon coupon = new Coupon("대박쿠폰", "설명", 100, 200,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        Coupon result = couponService.createCoupon(
                "대박쿠폰", "설명", 100, 200,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));

        assertThat(result.getName()).isEqualTo("대박쿠폰");
        assertThat(result.getRemainingQuantity()).isEqualTo(200);
        assertThat(result.getTotalQuantity()).isEqualTo(result.getRemainingQuantity());
    }
}
