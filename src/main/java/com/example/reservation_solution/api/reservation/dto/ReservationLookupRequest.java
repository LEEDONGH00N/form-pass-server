package com.example.reservation_solution.api.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationLookupRequest {
    private String guestName;
    private String guestPhoneNumber;
}
