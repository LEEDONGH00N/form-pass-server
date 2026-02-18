package com.example.reservation_solution.api.host.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ScheduleStatusResponse {
    private Long scheduleId;
    private String startTime;
    private String endTime;
    private Integer maxCapacity;
    private Integer currentCount;
    private List<SimpleReservationDto> reservations;
}
