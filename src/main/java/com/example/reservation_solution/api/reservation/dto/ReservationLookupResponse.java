package com.example.reservation_solution.api.reservation.dto;

import com.example.reservation_solution.api.reservation.domain.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReservationLookupResponse {
    private String qrToken;
    private String eventTitle;
    private String guestName;
    private Integer ticketCount;
    private LocalDateTime createdAt;

    public static ReservationLookupResponse from(Reservation reservation) {
        return new ReservationLookupResponse(
                reservation.getQrToken(),
                reservation.getEventSchedule().getEvent().getTitle(),
                reservation.getGuestName(),
                reservation.getTicketCount(),
                reservation.getCreatedAt()
        );
    }
}
