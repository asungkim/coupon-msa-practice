package com.example.coupon.domain.coupon.service;

import com.example.coupon.domain.coupon.dto.CouponIssueResponse;
import com.example.coupon.domain.coupon.event.CouponIssuedEvent;
import com.example.coupon.domain.coupon.service.strategy.CouponIssueStrategy;
import com.example.coupon.domain.coupon.entity.Coupon;
import com.example.coupon.domain.coupon.entity.CouponIssue;
import com.example.coupon.domain.coupon.enums.IssueStatus;
import com.example.coupon.domain.coupon.exception.CouponAlreadyIssuedException;
import com.example.coupon.domain.coupon.exception.CouponNotAvailableException;
import com.example.coupon.domain.coupon.exception.CouponNotFoundException;
import com.example.coupon.domain.coupon.repository.CouponIssueRepository;
import com.example.coupon.domain.coupon.repository.CouponRepository;
import com.example.coupon.domain.user.entity.User;
import com.example.coupon.domain.user.exception.UserNotFoundException;
import com.example.coupon.domain.user.repository.UserRepository;
import com.example.coupon.domain.user.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CouponServiceTest {

    private CouponRepository couponRepository;
    private CouponIssueRepository couponIssueRepository;
    private UserRepository userRepository;
    private PointService pointService;
    private ApplicationEventPublisher eventPublisher;
    private CouponIssueStrategy couponIssueStrategy;
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponRepository = mock(CouponRepository.class);
        couponIssueRepository = mock(CouponIssueRepository.class);
        userRepository = mock(UserRepository.class);
        pointService = mock(PointService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        couponIssueStrategy = mock(CouponIssueStrategy.class);
        couponService = new CouponService(couponRepository, couponIssueRepository,
                userRepository, pointService, eventPublisher, couponIssueStrategy);
    }

    private Coupon createAvailableCoupon() {
        return new Coupon("대박쿠폰", "설명", 100, 200,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
    }

    private User createUserWithPoints(Long points) {
        User user = new User("test@test.com", "홍길동");
        user.depositPoints(points);
        return user;
    }

    // === createCoupon ===

    @Test
    @DisplayName("쿠폰 생성 시 remainingQuantity == totalQuantity로 저장된다")
    void createCoupon() {
        Coupon coupon = createAvailableCoupon();
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        Coupon result = couponService.createCoupon(
                "대박쿠폰", "설명", 100, 200,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));

        assertThat(result.getRemainingQuantity()).isEqualTo(200);
    }

    // === issueCoupon 정상 ===

    @Test
    @DisplayName("정상 발급 시 CouponIssueResponse를 반환하고 재고가 1 감소한다")
    void issueCoupon_success() {
        Coupon coupon = createAvailableCoupon();
        User user = createUserWithPoints(10000L);
        CouponIssue savedIssue = new CouponIssue(1L, 1L, IssueStatus.ISSUED);

        when(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(couponIssueStrategy.decreaseQuantity(1L)).thenReturn(coupon);
        when(couponIssueRepository.save(any(CouponIssue.class))).thenReturn(savedIssue);

        CouponIssueResponse result = couponService.issueCoupon(1L, 1L);

        assertThat(result.status()).isEqualTo("ISSUED");
        verify(couponIssueStrategy).decreaseQuantity(1L);
        verify(pointService).decreasePoints(1L, 100L);
        verify(eventPublisher).publishEvent(any(CouponIssuedEvent.class));
    }

    // === issueCoupon 실패 케이스 ===

    @Test
    @DisplayName("중복 발급 시 CouponAlreadyIssuedException 발생")
    void issueCoupon_alreadyIssued() {
        when(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(CouponAlreadyIssuedException.class);
    }

    @Test
    @DisplayName("유저 미존재 시 UserNotFoundException 발생")
    void issueCoupon_userNotFound() {
        when(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("쿠폰 미존재 시 CouponNotFoundException 발생 (strategy에서)")
    void issueCoupon_couponNotFound() {
        when(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUserWithPoints(10000L)));
        when(couponIssueStrategy.decreaseQuantity(1L))
                .thenThrow(new CouponNotFoundException(1L));

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("발급 불가(재고 0) 시 CouponNotAvailableException 발생 (strategy에서)")
    void issueCoupon_notAvailable() {
        when(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUserWithPoints(10000L)));
        when(couponIssueStrategy.decreaseQuantity(1L))
                .thenThrow(new CouponNotAvailableException(1L, "not available"));

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(CouponNotAvailableException.class);
    }

    @Test
    @DisplayName("포인트 부족 시 InsufficientPointsException 전파")
    void issueCoupon_insufficientPoints() {
        Coupon coupon = createAvailableCoupon();

        when(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUserWithPoints(10000L)));
        when(couponIssueStrategy.decreaseQuantity(1L)).thenReturn(coupon);
        doThrow(new com.example.coupon.domain.user.exception.InsufficientPointsException(1L, 100L, 50L))
                .when(pointService).decreasePoints(anyLong(), anyLong());

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(com.example.coupon.domain.user.exception.InsufficientPointsException.class);
    }
}
