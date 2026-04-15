package com.example.coupon.domain.coupon.service.strategy;

import com.example.coupon.domain.coupon.entity.Coupon;
import com.example.coupon.domain.coupon.exception.CouponNotAvailableException;
import com.example.coupon.domain.coupon.exception.CouponNotFoundException;
import com.example.coupon.domain.coupon.exception.CouponOutOfStockException;
import com.example.coupon.domain.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "coupon.issue.strategy", havingValue = "atomic", matchIfMissing = true)
@RequiredArgsConstructor
public class AtomicUpdateIssueStrategy implements CouponIssueStrategy {

    private final CouponRepository couponRepository;

    @Override
    public Coupon decreaseQuantity(Long couponId) {
        log.info("Atomic update: decreasing quantity for coupon {}", couponId);

        var coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException(couponId));

        if (!coupon.isAvailable()) {
            throw new CouponNotAvailableException(couponId, "Coupon is not available or expired");
        }

        int affected = couponRepository.decreaseQuantity(couponId);
        if (affected == 0) {
            throw new CouponOutOfStockException(couponId);
        }

        return coupon;
    }
}
