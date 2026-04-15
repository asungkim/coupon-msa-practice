package com.example.coupon.integration;

import com.example.coupon.domain.coupon.client.PushNotificationClient;
import com.example.coupon.domain.coupon.entity.Coupon;
import com.example.coupon.domain.coupon.repository.CouponRepository;
import com.example.coupon.domain.user.entity.User;
import com.example.coupon.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CouponIssueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    @MockitoBean
    private PushNotificationClient pushNotificationClient;

    private User savedUser;
    private Coupon savedCoupon;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        couponRepository.deleteAll();

        savedUser = userRepository.save(new User("test@test.com", "홍길동"));
        savedUser.depositPoints(10000L);
        savedUser = userRepository.save(savedUser);

        savedCoupon = couponRepository.save(new Coupon("대박쿠폰", "설명", 100, 200,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30)));
    }

    @Test
    @DisplayName("정상 발급 — 유저 포인트 차감 + 재고 감소 + ISSUED 응답")
    void issueCoupon_success() throws Exception {
        mockMvc.perform(post("/api/coupons/" + savedCoupon.getId() + "/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + savedUser.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.userId").value(savedUser.getId()))
                .andExpect(jsonPath("$.couponId").value(savedCoupon.getId()));
    }

    @Test
    @DisplayName("중복 발급 — 같은 유저가 같은 쿠폰 2번 발급 시 409")
    void issueCoupon_duplicate() throws Exception {
        mockMvc.perform(post("/api/coupons/" + savedCoupon.getId() + "/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + savedUser.getId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/coupons/" + savedCoupon.getId() + "/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + savedUser.getId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Coupon already issued to this user"));
    }

    @Test
    @DisplayName("재고 소진 — totalQuantity=1 쿠폰, 2번째 요청 시 400")
    void issueCoupon_outOfStock() throws Exception {
        Coupon limitedCoupon = couponRepository.save(new Coupon("한정쿠폰", "설명", 100, 1,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30)));

        mockMvc.perform(post("/api/coupons/" + limitedCoupon.getId() + "/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + savedUser.getId() + "}"))
                .andExpect(status().isOk());

        User user2 = userRepository.save(new User("test2@test.com", "김철수"));
        user2.depositPoints(10000L);
        userRepository.save(user2);

        mockMvc.perform(post("/api/coupons/" + limitedCoupon.getId() + "/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + user2.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Coupon is not available or expired"));
    }

    @Test
    @DisplayName("포인트 부족 — 포인트 0인 유저 발급 시 400")
    void issueCoupon_insufficientPoints() throws Exception {
        User poorUser = userRepository.save(new User("poor@test.com", "가난이"));

        mockMvc.perform(post("/api/coupons/" + savedCoupon.getId() + "/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + poorUser.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient points"));
    }
}
