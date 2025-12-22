package com.example.reservation_solution.dto;

public record CheckinResponse(
    String message,
    String guestName,
    Integer ticketCount
) {
}
