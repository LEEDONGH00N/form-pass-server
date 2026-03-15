package com.example.reservation_solution.api.reservation.controller;

import com.example.reservation_solution.api.reservation.dto.ReservationRequest;
import com.example.reservation_solution.api.reservation.dto.ReservationResponse;
import com.example.reservation_solution.api.reservation.service.ReservationFacade;
import com.example.reservation_solution.api.reservation.service.ReservationService;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private ReservationFacade reservationFacade;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("예약 생성 성공 시 201을 반환한다")
    void createReservation_shouldReturn201() throws Exception {
        // given
        ReservationRequest request = new ReservationRequest(1L, "홍길동", "01012345678", 1, null);
        ReservationResponse mockResponse = new ReservationResponse(
                1L, "qr-token", "홍길동", "01012345678", 1,
                com.example.reservation_solution.api.reservation.domain.ReservationStatus.CONFIRMED,
                false, "이벤트", "서울", null, Collections.emptyList(), null
        );

        given(reservationFacade.createReservation(any())).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guestName").value("홍길동"));
    }

    @Test
    @DisplayName("이름이 빈값이면 400을 반환한다")
    void createReservation_shouldReturn400_whenNameIsBlank() throws Exception {
        // given
        ReservationRequest request = new ReservationRequest(1L, "", "01012345678", 1, null);

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("전화번호 형식이 올바르지 않으면 400을 반환한다")
    void createReservation_shouldReturn400_whenPhoneInvalid() throws Exception {
        // given
        ReservationRequest request = new ReservationRequest(1L, "홍길동", "12345", 1, null);

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("티켓 수가 0이면 400을 반환한다")
    void createReservation_shouldReturn400_whenTicketCountIsZero() throws Exception {
        // given
        ReservationRequest request = new ReservationRequest(1L, "홍길동", "01012345678", 0, null);

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예약 단건 조회 시 200을 반환한다")
    void getReservation_shouldReturn200() throws Exception {
        // given
        ReservationResponse mockResponse = new ReservationResponse(
                1L, "qr-token", "홍길동", "01012345678", 1,
                com.example.reservation_solution.api.reservation.domain.ReservationStatus.CONFIRMED,
                false, "이벤트", "서울", null, Collections.emptyList(), null
        );

        given(reservationService.getReservation(1L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("예약 취소 시 204를 반환한다")
    void cancelReservation_shouldReturn204() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isNoContent());
    }
}
