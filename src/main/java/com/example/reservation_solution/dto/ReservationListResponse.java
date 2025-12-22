package com.example.reservation_solution.dto;

import com.example.reservation_solution.domain.Reservation;
import com.example.reservation_solution.domain.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReservationListResponse {
    private Long id;
    private String guestName;
    private String guestPhoneNumber;
    private Integer ticketCount;
    private ReservationStatus status;
    private Boolean isCheckedIn;
    private LocalDateTime createdAt;
    private String scheduleName;

    public static ReservationListResponse from(Reservation reservation) {
        String scheduleName = reservation.getEventSchedule().getStartTime().toString() +
                " ~ " + reservation.getEventSchedule().getEndTime().toString();

        return new ReservationListResponse(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestPhoneNumber(),
                reservation.getTicketCount(),
                reservation.getStatus(),
                reservation.isCheckedIn(),
                reservation.getCreatedAt(),
                scheduleName
        );
    }
}
