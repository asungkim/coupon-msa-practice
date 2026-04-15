package com.example.coupon.global.exception;

import com.example.coupon.domain.coupon.exception.*;
import com.example.coupon.domain.user.exception.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, "User not found");
    }

    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCouponNotFound(CouponNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, "Coupon not found");
    }

    @ExceptionHandler(CouponAlreadyIssuedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyIssued(CouponAlreadyIssuedException e) {
        return buildResponse(HttpStatus.CONFLICT, "Coupon already issued to this user");
    }

    @ExceptionHandler(CouponOutOfStockException.class)
    public ResponseEntity<Map<String, Object>> handleOutOfStock(CouponOutOfStockException e) {
        return buildResponse(HttpStatus.CONFLICT, "Coupon out of stock");
    }

    @ExceptionHandler(CouponNotAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleNotAvailable(CouponNotAvailableException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Coupon is not available or expired");
    }

    @ExceptionHandler(InsufficientPointsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientPoints(InsufficientPointsException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Insufficient points");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        return buildResponse(HttpStatus.CONFLICT, "Coupon already issued to this user");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "message", message
        ));
    }
}
