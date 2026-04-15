package com.example.coupon.domain.user.service;

import com.example.coupon.domain.user.entity.User;
import com.example.coupon.domain.user.exception.InsufficientPointsException;
import com.example.coupon.domain.user.exception.UserNotFoundException;
import com.example.coupon.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PointServiceTest {

    private UserRepository userRepository;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        pointService = new PointService(userRepository);
    }

    @Test
    @DisplayName("포인트 차감 시 유저의 포인트가 줄어든다")
    void decreasePoints() {
        User user = new User("test@test.com", "홍길동");
        user.depositPoints(1000L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        pointService.decreasePoints(1L, 300L);

        assertThat(user.getPoint()).isEqualTo(700L);
    }

    @Test
    @DisplayName("유저 미존재 시 UserNotFoundException 발생")
    void decreasePoints_userNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointService.decreasePoints(999L, 100L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("잔액 부족 시 InsufficientPointsException 발생")
    void decreasePoints_insufficientPoints() {
        User user = new User("test@test.com", "홍길동");
        user.depositPoints(50L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> pointService.decreasePoints(1L, 100L))
                .isInstanceOf(InsufficientPointsException.class);
    }
}
