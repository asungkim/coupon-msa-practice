package com.example.coupon.domain.coupon.controller;

import com.example.coupon.domain.coupon.entity.Coupon;
import com.example.coupon.domain.coupon.service.CouponService;
import com.example.coupon.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CouponControllerTest {

    private MockMvc mockMvc;
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponService = mock(CouponService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CouponController(couponService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

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
                .andExpect(jsonPath("$.remainingQuantity").value(200))
                .andExpect(jsonPath("$.pointCost").value(100));
    }
}
