package com.example.coupon.domain.coupon.entity;

import com.example.coupon.domain.coupon.exception.CouponOutOfStockException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer pointCost;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer remainingQuantity;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    public Coupon(String name, String description, Integer pointCost,
                  Integer totalQuantity, LocalDateTime startDate, LocalDateTime endDate) {
        this.name = name;
        this.description = description;
        this.pointCost = pointCost;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean isAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return remainingQuantity > 0
                && now.isAfter(startDate)
                && now.isBefore(endDate);
    }

    public void decreaseQuantity() {
        if (remainingQuantity <= 0) {
            throw new CouponOutOfStockException(id);
        }
        remainingQuantity--;
    }
}
