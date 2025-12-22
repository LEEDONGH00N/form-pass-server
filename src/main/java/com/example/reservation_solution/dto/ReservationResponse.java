package com.example.reservation_solution.dto;

import com.example.reservation_solution.domain.Reservation;
import com.example.reservation_solution.domain.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private String qrToken;
    private String guestName;
    private String guestPhoneNumber;
    private Integer ticketCount;
    private ReservationStatus status;
    private Boolean isCheckedIn;
    private String eventTitle;
    private String eventLocation;
    private ScheduleResponse schedule;
    private List<FormAnswerResponse> answers;
    private LocalDateTime createdAt;

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getQrToken(),
                reservation.getGuestName(),
                reservation.getGuestPhoneNumber(),
                reservation.getTicketCount(),
                reservation.getStatus(),
                reservation.isCheckedIn(),
                reservation.getEventSchedule().getEvent().getTitle(),
                reservation.getEventSchedule().getEvent().getLocation(),
                ScheduleResponse.from(reservation.getEventSchedule()),
                reservation.getFormAnswers().stream()
                        .map(FormAnswerResponse::from)
                        .collect(Collectors.toList()),
                reservation.getCreatedAt()
        );
    }
}
