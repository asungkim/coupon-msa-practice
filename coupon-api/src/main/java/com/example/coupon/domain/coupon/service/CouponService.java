package com.example.coupon.domain.coupon.service;

import com.example.coupon.domain.coupon.client.PushNotificationClient;
import com.example.coupon.domain.coupon.dto.CouponIssueResponse;
import com.example.coupon.domain.coupon.entity.Coupon;
import com.example.coupon.domain.coupon.entity.CouponIssue;
import com.example.coupon.domain.coupon.enums.IssueStatus;
import com.example.coupon.domain.coupon.exception.CouponAlreadyIssuedException;
import com.example.coupon.domain.coupon.exception.CouponNotAvailableException;
import com.example.coupon.domain.coupon.exception.CouponNotFoundException;
import com.example.coupon.domain.coupon.repository.CouponIssueRepository;
import com.example.coupon.domain.coupon.repository.CouponRepository;
import com.example.coupon.domain.user.exception.UserNotFoundException;
import com.example.coupon.domain.user.repository.UserRepository;
import com.example.coupon.domain.user.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final PushNotificationClient pushNotificationClient;

    @Transactional
    public Coupon createCoupon(String name, String description, Integer pointCost,
                               Integer totalQuantity, LocalDateTime startDate, LocalDateTime endDate) {
        Coupon coupon = new Coupon(name, description, pointCost, totalQuantity, startDate, endDate);
        return couponRepository.save(coupon);
    }

    @Transactional
    public CouponIssueResponse issueCoupon(Long userId, Long couponId) {
        log.info("User {} attempting to issue coupon {}", userId, couponId);

        if (couponIssueRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new CouponAlreadyIssuedException(userId, couponId);
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        var coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException(couponId));

        if (!coupon.isAvailable()) {
            throw new CouponNotAvailableException(couponId, "Coupon is not available or expired");
        }

        pointService.decreasePoints(userId, (long) coupon.getPointCost());

        coupon.decreaseQuantity();

        var issue = couponIssueRepository.save(
                new CouponIssue(userId, couponId, IssueStatus.ISSUED));

        pushNotificationClient.sendCouponIssuedNotification(
                userId, coupon.getName(), coupon.getId());

        log.info("Coupon {} issued to user {} successfully", couponId, userId);

        return CouponIssueResponse.from(issue);
    }
}
