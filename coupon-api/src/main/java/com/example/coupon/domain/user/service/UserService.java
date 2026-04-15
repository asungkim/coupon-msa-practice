package com.example.coupon.domain.user.service;

import com.example.coupon.domain.user.entity.User;
import com.example.coupon.domain.user.exception.UserNotFoundException;
import com.example.coupon.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User createUser(String email, String name) {
        User user = new User(email, name);
        return userRepository.save(user);
    }

    @Transactional
    public User depositPoints(Long userId, Long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.depositPoints(amount);
        return user;
    }
}
