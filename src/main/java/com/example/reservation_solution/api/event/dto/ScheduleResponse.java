package com.example.reservation_solution.api.event.dto;

import com.example.reservation_solution.api.event.domain.EventSchedule;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleResponse {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxCapacity;
    private Integer reservedCount;

    public static ScheduleResponse from(EventSchedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getMaxCapacity(),
                schedule.getReservedCount()
        );
    }
}
