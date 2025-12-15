package com.example.reservation_solution.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    private Long scheduleId;
    private String guestName;
    private String guestPhoneNumber;
    private Integer ticketCount;
    private List<FormAnswerRequest> answers;
}
