package com.example.reservation_solution.dto;

import jakarta.validation.constraints.Min;
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
    @Min(1) private Integer ticketCount;
    private List<FormAnswerRequest> answers;
}
