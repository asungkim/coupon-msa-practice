package com.example.coupon.domain.user.dto;

import com.example.coupon.domain.user.entity.User;

public record UserResponse(Long id, String email, String name, Long point) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getPoint());
    }
}
