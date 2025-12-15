package com.example.reservation_solution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startTime;

    @NotNull(message = "종료 시간은 필수입니다")
    private LocalDateTime endTime;

    @NotNull(message = "최대 정원은 필수입니다")
    private Integer maxCapacity;
}
