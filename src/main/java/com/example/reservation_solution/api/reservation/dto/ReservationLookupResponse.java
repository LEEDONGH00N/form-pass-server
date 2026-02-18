package com.example.reservation_solution.api.reservation.dto;

import com.example.reservation_solution.api.event.domain.EventSchedule;
import com.example.reservation_solution.api.reservation.domain.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReservationLookupResponse {
    private Long id;
    private String qrToken;
    private String eventTitle;
    private String eventLocation;
    private String guestName;
    private Integer ticketCount;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private Boolean isCheckedIn;
    private LocalDateTime createdAt;

    public static ReservationLookupResponse from(Reservation reservation) {
        EventSchedule schedule = reservation.getEventSchedule();
        return new ReservationLookupResponse(
                reservation.getId(),
                reservation.getQrToken(),
                schedule.getEvent().getTitle(),
                schedule.getEvent().getLocation(),
                reservation.getGuestName(),
                reservation.getTicketCount(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                reservation.getIsCheckedIn(),
                reservation.getCreatedAt()
        );
    }
}
