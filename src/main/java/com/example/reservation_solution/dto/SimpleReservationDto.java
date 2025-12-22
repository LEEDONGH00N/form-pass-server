package com.example.reservation_solution.dto;

import com.example.reservation_solution.domain.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimpleReservationDto {
    private Long id;
    private String guestName;
    private String guestPhoneNumber;
    private Integer ticketCount;
    private Boolean isCheckedIn;

    public static SimpleReservationDto from(Reservation reservation) {
        return new SimpleReservationDto(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestPhoneNumber(),
                reservation.getTicketCount(),
                reservation.isCheckedIn()
        );
    }
}
