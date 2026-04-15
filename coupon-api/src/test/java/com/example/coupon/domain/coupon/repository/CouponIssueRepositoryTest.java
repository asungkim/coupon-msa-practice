package com.example.coupon.domain.coupon.repository;

import com.example.coupon.domain.coupon.entity.CouponIssue;
import com.example.coupon.domain.coupon.enums.IssueStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class CouponIssueRepositoryTest {

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Test
    @DisplayName("existsByUserIdAndCouponId - 존재하면 true")
    void existsByUserIdAndCouponId_true() {
        couponIssueRepository.save(new CouponIssue(1L, 1L, IssueStatus.ISSUED));

        assertThat(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndCouponId - 존재하지 않으면 false")
    void existsByUserIdAndCouponId_false() {
        assertThat(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).isFalse();
    }

    @Test
    @DisplayName("같은 userId + couponId로 중복 저장 시 예외 발생 (UNIQUE 제약)")
    void uniqueConstraint() {
        couponIssueRepository.saveAndFlush(new CouponIssue(1L, 1L, IssueStatus.ISSUED));

        assertThatThrownBy(() ->
                couponIssueRepository.saveAndFlush(new CouponIssue(1L, 1L, IssueStatus.ISSUED))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
