package com.example.reservation_solution.api.reservation.dto;

import com.example.reservation_solution.api.event.dto.ScheduleResponse;
import com.example.reservation_solution.api.reservation.domain.Reservation;
import com.example.reservation_solution.api.reservation.domain.ReservationStatus;
import com.example.reservation_solution.global.util.EncryptionUtils;
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

    public static ReservationResponse from(Reservation reservation, EncryptionUtils encryptionUtils) {
        // 전화번호 복호화
        String decryptedPhoneNumber = encryptionUtils.decrypt(reservation.getGuestPhoneNumber());

        return new ReservationResponse(
                reservation.getId(),
                reservation.getQrToken(),
                reservation.getGuestName(),
                decryptedPhoneNumber,  // 복호화된 전화번호 반환
                reservation.getTicketCount(),
                reservation.getStatus(),
                reservation.getIsCheckedIn(),
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
