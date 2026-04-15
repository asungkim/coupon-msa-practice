package com.example.coupon.domain.coupon.service.strategy;

import com.example.coupon.domain.coupon.entity.Coupon;
import com.example.coupon.domain.coupon.exception.CouponNotAvailableException;
import com.example.coupon.domain.coupon.exception.CouponNotFoundException;
import com.example.coupon.domain.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "coupon.issue.strategy", havingValue = "pessimistic")
@RequiredArgsConstructor
public class PessimisticLockIssueStrategy implements CouponIssueStrategy {

    private final CouponRepository couponRepository;

    @Override
    public Coupon decreaseQuantity(Long couponId) {
        log.info("Pessimistic lock: acquiring lock for coupon {}", couponId);

        var coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new CouponNotFoundException(couponId));

        if (!coupon.isAvailable()) {
            throw new CouponNotAvailableException(couponId, "Coupon is not available or expired");
        }

        coupon.decreaseQuantity();
        return coupon;
    }
}
