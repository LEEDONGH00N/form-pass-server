package com.example.reservation_solution.dto;

public record DashboardResponse(
    Integer totalSeats,
    Integer reservedCount,
    Double reservationRate,
    Integer checkedInCount,
    Integer availableSeats
) {
}
