package com.example.coupon.domain.user.exception;

public class InsufficientPointsException extends RuntimeException {

    public InsufficientPointsException(Long userId, Long required, Long current) {
        super("Insufficient points: user " + userId + " requires " + required + " but has " + current);
    }
}
