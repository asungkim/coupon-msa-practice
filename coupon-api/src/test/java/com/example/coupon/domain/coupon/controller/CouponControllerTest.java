package com.example.coupon.domain.coupon.controller;

import com.example.coupon.domain.coupon.dto.CouponIssueResponse;
import com.example.coupon.domain.coupon.entity.Coupon;
import com.example.coupon.domain.coupon.exception.CouponAlreadyIssuedException;
import com.example.coupon.domain.coupon.exception.CouponNotAvailableException;
import com.example.coupon.domain.coupon.exception.CouponNotFoundException;
import com.example.coupon.domain.coupon.service.CouponService;
import com.example.coupon.domain.user.exception.InsufficientPointsException;
import com.example.coupon.domain.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

    // === 쿠폰 생성 ===

    @Test
    @DisplayName("POST /api/coupons — 쿠폰 생성 성공")
    void createCoupon() throws Exception {
        Coupon coupon = new Coupon("대박쿠폰", "이벤트 오픈 기념", 100, 200,
                LocalDateTime.of(2026, 4, 14, 0, 0),
                LocalDateTime.of(2026, 5, 14, 0, 0));
        when(couponService.createCoupon(anyString(), anyString(), anyInt(), anyInt(),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(coupon);

        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "대박쿠폰",
                                    "description": "이벤트 오픈 기념",
                                    "pointCost": 100,
                                    "totalQuantity": 200,
                                    "startDate": "2026-04-14T00:00:00",
                                    "endDate": "2026-05-14T00:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("대박쿠폰"))
                .andExpect(jsonPath("$.totalQuantity").value(200))
                .andExpect(jsonPath("$.remainingQuantity").value(200));
    }

    // === 쿠폰 발급 ===

    @Test
    @DisplayName("POST /api/coupons/{couponId}/issue — 발급 성공")
    void issueCoupon_success() throws Exception {
        var response = new CouponIssueResponse(1L, 1L, 1L, "ISSUED",
                LocalDateTime.of(2026, 4, 15, 12, 0));
        when(couponService.issueCoupon(1L, 1L)).thenReturn(response);

        mockMvc.perform(post("/api/coupons/1/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.couponId").value(1))
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    @DisplayName("중복 발급 시 409")
    void issueCoupon_alreadyIssued() throws Exception {
        when(couponService.issueCoupon(anyLong(), anyLong()))
                .thenThrow(new CouponAlreadyIssuedException(1L, 1L));

        mockMvc.perform(post("/api/coupons/1/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Coupon already issued to this user"));
    }

    @Test
    @DisplayName("유저 미존재 시 404")
    void issueCoupon_userNotFound() throws Exception {
        when(couponService.issueCoupon(anyLong(), anyLong()))
                .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(post("/api/coupons/1/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 999}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("쿠폰 미존재 시 404")
    void issueCoupon_couponNotFound() throws Exception {
        when(couponService.issueCoupon(anyLong(), anyLong()))
                .thenThrow(new CouponNotFoundException(999L));

        mockMvc.perform(post("/api/coupons/999/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Coupon not found"));
    }

    @Test
    @DisplayName("발급 불가(기간 외/재고 0) 시 400")
    void issueCoupon_notAvailable() throws Exception {
        when(couponService.issueCoupon(anyLong(), anyLong()))
                .thenThrow(new CouponNotAvailableException(1L, "expired"));

        mockMvc.perform(post("/api/coupons/1/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Coupon is not available or expired"));
    }

    @Test
    @DisplayName("포인트 부족 시 400")
    void issueCoupon_insufficientPoints() throws Exception {
        when(couponService.issueCoupon(anyLong(), anyLong()))
                .thenThrow(new InsufficientPointsException(1L, 100L, 50L));

        mockMvc.perform(post("/api/coupons/1/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient points"));
    }
}
