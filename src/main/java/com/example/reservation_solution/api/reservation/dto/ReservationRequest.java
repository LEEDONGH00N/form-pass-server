package com.example.reservation_solution.api.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    @NotNull(message = "스케줄 ID는 필수입니다")
    private Long scheduleId;

    @NotBlank(message = "이름은 필수입니다")
    private String guestName;

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]\\d{7,8}$", message = "올바른 전화번호 형식이 아닙니다")
    private String guestPhoneNumber;

    @NotNull(message = "티켓 수는 필수입니다")
    @Min(value = 1, message = "티켓 수는 최소 1개 이상이어야 합니다")
    private Integer ticketCount;

    private List<FormAnswerRequest> answers;
}
