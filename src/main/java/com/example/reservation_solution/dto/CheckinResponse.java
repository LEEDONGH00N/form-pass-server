package com.example.reservation_solution.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckinResponse {
    private String message;
    private String guestName;
    private Integer ticketCount;
}
