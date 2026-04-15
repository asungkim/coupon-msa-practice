package com.example.coupon.domain.user.controller;

import com.example.coupon.domain.user.dto.CreateUserRequest;
import com.example.coupon.domain.user.dto.DepositPointsRequest;
import com.example.coupon.domain.user.dto.UserResponse;
import com.example.coupon.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public UserResponse createUser(@RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createUser(request.email(), request.name()));
    }

    @PostMapping("/{userId}/points")
    public UserResponse depositPoints(@PathVariable Long userId,
                                      @RequestBody DepositPointsRequest request) {
        return UserResponse.from(userService.depositPoints(userId, request.amount()));
    }
}
