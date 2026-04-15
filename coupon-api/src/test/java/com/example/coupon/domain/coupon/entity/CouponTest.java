package com.example.coupon.domain.coupon.entity;

import com.example.coupon.domain.coupon.exception.CouponOutOfStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private Coupon createCoupon(int totalQuantity, LocalDateTime startDate, LocalDateTime endDate) {
        return new Coupon("테스트쿠폰", "설명", 100, totalQuantity, startDate, endDate);
    }

    @Test
    @DisplayName("생성 시 remainingQuantity는 totalQuantity와 같다")
    void initialRemainingQuantity() {
        Coupon coupon = createCoupon(200,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30));

        assertThat(coupon.getRemainingQuantity()).isEqualTo(200);
    }

    @Test
    @DisplayName("재고 있고 기간 내이면 isAvailable은 true")
    void isAvailable_true() {
        Coupon coupon = createCoupon(200,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30));

        assertThat(coupon.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("재고 0이면 isAvailable은 false")
    void isAvailable_outOfStock() {
        Coupon coupon = createCoupon(0,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30));

        assertThat(coupon.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("시작일 전이면 isAvailable은 false")
    void isAvailable_beforeStartDate() {
        Coupon coupon = createCoupon(200,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30));

        assertThat(coupon.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("종료일 후이면 isAvailable은 false")
    void isAvailable_afterEndDate() {
        Coupon coupon = createCoupon(200,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(1));

        assertThat(coupon.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("decreaseQuantity 호출 시 재고가 1 감소한다")
    void decreaseQuantity() {
        Coupon coupon = createCoupon(200,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30));

        coupon.decreaseQuantity();

        assertThat(coupon.getRemainingQuantity()).isEqualTo(199);
    }

    @Test
    @DisplayName("재고 0일 때 decreaseQuantity 호출 시 CouponOutOfStockException 발생")
    void decreaseQuantity_outOfStock() {
        Coupon coupon = createCoupon(0,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30));

        assertThatThrownBy(coupon::decreaseQuantity)
                .isInstanceOf(CouponOutOfStockException.class);
    }
}
