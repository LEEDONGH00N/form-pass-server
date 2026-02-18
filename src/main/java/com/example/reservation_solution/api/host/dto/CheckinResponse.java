package com.example.reservation_solution.api.host.dto;

public record CheckinResponse(
    String message,
    String guestName,
    Integer ticketCount
) {
}
