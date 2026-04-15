package com.example.coupon.domain.user.service;

import com.example.coupon.domain.user.entity.User;
import com.example.coupon.domain.user.exception.UserNotFoundException;
import com.example.coupon.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    @DisplayName("유저 생성 시 저장된 유저를 반환한다")
    void createUser() {
        User user = new User("test@test.com", "홍길동");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.createUser("test@test.com", "홍길동");

        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getPoint()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트 충전 시 기존 포인트에 누적된다")
    void depositPoints() {
        User user = new User("test@test.com", "홍길동");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.depositPoints(1L, 1000L);

        assertThat(user.getPoint()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("포인트 충전 시 유저 미존재이면 UserNotFoundException 발생")
    void depositPoints_userNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.depositPoints(999L, 1000L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
