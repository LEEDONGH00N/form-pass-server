package com.example.reservation_solution.dto;

import com.example.reservation_solution.domain.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private String location;
    private String thumbnailUrl;
    private String description;
    private List<ScheduleResponse> schedules;
    private List<QuestionResponse> questions;

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getLocation(),
                event.getThumbnailUrl(),
                event.getDescription(),
                event.getSchedules().stream()
                        .map(ScheduleResponse::from)
                        .collect(Collectors.toList()),
                event.getQuestions().stream()
                        .map(QuestionResponse::from)
                        .collect(Collectors.toList())
        );
    }
}
