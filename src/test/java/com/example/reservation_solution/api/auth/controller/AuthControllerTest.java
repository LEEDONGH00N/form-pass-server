package com.example.reservation_solution.api.auth.controller;

import com.example.reservation_solution.api.auth.dto.LoginRequest;
import com.example.reservation_solution.api.auth.dto.LoginResponse;
import com.example.reservation_solution.api.auth.dto.SignupRequest;
import com.example.reservation_solution.api.auth.service.AuthService;
import com.example.reservation_solution.global.security.JwtAuthenticationFilter;
import com.example.reservation_solution.global.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("회원가입 성공 시 201을 반환한다")
    void signup_shouldReturn201() throws Exception {
        // given
        SignupRequest request = new SignupRequest("test@example.com", "password123", "테스트");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400을 반환한다")
    void signup_shouldReturn400_whenEmailInvalid() throws Exception {
        // given
        SignupRequest request = new SignupRequest("invalid-email", "password123", "테스트");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공 시 200과 토큰을 반환한다")
    void login_shouldReturn200() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        given(authService.generateLoginToken(any())).willReturn("jwt-token");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    @DisplayName("비밀번호가 빈값이면 400을 반환한다")
    void login_shouldReturn400_whenPasswordBlank() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
