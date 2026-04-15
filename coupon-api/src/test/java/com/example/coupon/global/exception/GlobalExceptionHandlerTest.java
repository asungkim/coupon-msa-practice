package com.example.coupon.global.exception;

import com.example.coupon.domain.coupon.exception.*;
import com.example.coupon.domain.user.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {

        @GetMapping("/test/user-not-found")
        void userNotFound() { throw new UserNotFoundException(1L); }

        @GetMapping("/test/coupon-not-found")
        void couponNotFound() { throw new CouponNotFoundException(1L); }

        @GetMapping("/test/already-issued")
        void alreadyIssued() { throw new CouponAlreadyIssuedException(1L, 1L); }

        @GetMapping("/test/out-of-stock")
        void outOfStock() { throw new CouponOutOfStockException(1L); }

        @GetMapping("/test/not-available")
        void notAvailable() { throw new CouponNotAvailableException(1L, "expired"); }

        @GetMapping("/test/insufficient-points")
        void insufficientPoints() { throw new InsufficientPointsException(1L, 100L, 50L); }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("UserNotFoundException → 404")
    void userNotFound() throws Exception {
        mockMvc.perform(get("/test/user-not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("CouponNotFoundException → 404")
    void couponNotFound() throws Exception {
        mockMvc.perform(get("/test/coupon-not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Coupon not found"));
    }

    @Test
    @DisplayName("CouponAlreadyIssuedException → 409")
    void alreadyIssued() throws Exception {
        mockMvc.perform(get("/test/already-issued").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Coupon already issued to this user"));
    }

    @Test
    @DisplayName("CouponOutOfStockException → 409")
    void outOfStock() throws Exception {
        mockMvc.perform(get("/test/out-of-stock").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Coupon out of stock"));
    }

    @Test
    @DisplayName("CouponNotAvailableException → 400")
    void notAvailable() throws Exception {
        mockMvc.perform(get("/test/not-available").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Coupon is not available or expired"));
    }

    @Test
    @DisplayName("InsufficientPointsException → 400")
    void insufficientPoints() throws Exception {
        mockMvc.perform(get("/test/insufficient-points").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient points"));
    }
}
