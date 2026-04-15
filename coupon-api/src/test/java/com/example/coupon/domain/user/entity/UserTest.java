package com.example.coupon.domain.user.entity;

import com.example.coupon.domain.user.exception.InsufficientPointsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("포인트 충전 시 기존 포인트에 누적된다")
    void depositPoints() {
        User user = new User("test@test.com", "홍길동");

        user.depositPoints(1000L);
        user.depositPoints(500L);

        assertThat(user.getPoint()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("포인트 차감 시 잔액이 줄어든다")
    void usePoints() {
        User user = new User("test@test.com", "홍길동");
        user.depositPoints(1000L);

        user.usePoints(300L);

        assertThat(user.getPoint()).isEqualTo(700L);
    }

    @Test
    @DisplayName("포인트 잔액 부족 시 InsufficientPointsException 발생")
    void usePoints_insufficient() {
        User user = new User("test@test.com", "홍길동");
        user.depositPoints(100L);

        assertThatThrownBy(() -> user.usePoints(200L))
                .isInstanceOf(InsufficientPointsException.class);
    }

    @Test
    @DisplayName("유저 생성 시 포인트는 0이다")
    void initialPoint() {
        User user = new User("test@test.com", "홍길동");

        assertThat(user.getPoint()).isEqualTo(0L);
    }
}
