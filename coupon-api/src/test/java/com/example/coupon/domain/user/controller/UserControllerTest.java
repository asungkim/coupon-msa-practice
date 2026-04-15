package com.example.coupon.domain.user.controller;

import com.example.coupon.domain.user.entity.User;
import com.example.coupon.domain.user.exception.UserNotFoundException;
import com.example.coupon.domain.user.service.UserService;
import com.example.coupon.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/users — 유저 생성 성공")
    void createUser() throws Exception {
        User user = new User("test@test.com", "홍길동");
        when(userService.createUser(anyString(), anyString())).thenReturn(user);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@test.com", "name": "홍길동"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.point").value(0));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/points — 포인트 충전 성공")
    void depositPoints() throws Exception {
        User user = new User("test@test.com", "홍길동");
        user.depositPoints(10000L);
        when(userService.depositPoints(1L, 10000L)).thenReturn(user);

        mockMvc.perform(post("/api/users/1/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(10000));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/points — 유저 미존재 시 404")
    void depositPoints_notFound() throws Exception {
        when(userService.depositPoints(anyLong(), anyLong()))
                .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(post("/api/users/999/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10000}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
