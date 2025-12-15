package com.example.reservation_solution.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardResponse {
    private Integer totalSeats;
    private Integer reservedCount;
    private Double reservationRate;
    private Integer checkedInCount;
    private Integer availableSeats;
}
