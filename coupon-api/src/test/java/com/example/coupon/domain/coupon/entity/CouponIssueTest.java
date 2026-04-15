package com.example.coupon.domain.coupon.entity;

import com.example.coupon.domain.coupon.enums.IssueStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CouponIssueTest {

    @Test
    @DisplayName("생성 시 status는 ISSUED이고 issuedAt이 설정된다")
    void create() {
        CouponIssue issue = new CouponIssue(1L, 1L, IssueStatus.ISSUED);

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.ISSUED);
        assertThat(issue.getIssuedAt()).isNotNull();
        assertThat(issue.getUserId()).isEqualTo(1L);
        assertThat(issue.getCouponId()).isEqualTo(1L);
    }
}
