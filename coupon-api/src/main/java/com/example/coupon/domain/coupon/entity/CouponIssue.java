package com.example.coupon.domain.coupon.entity;

import com.example.coupon.domain.coupon.enums.IssueStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_coupon_issue_user_coupon",
        columnNames = {"userId", "couponId"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    public CouponIssue(Long userId, Long couponId, IssueStatus status) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = status;
        this.issuedAt = LocalDateTime.now();
    }
}
