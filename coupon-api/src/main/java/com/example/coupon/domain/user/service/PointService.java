package com.example.coupon.domain.user.service;

import com.example.coupon.domain.user.exception.UserNotFoundException;
import com.example.coupon.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;

    @Transactional
    public void decreasePoints(Long userId, Long amount) {
        log.info("Deducting {} points from user {}", amount, userId);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.usePoints(amount);
    }
}
