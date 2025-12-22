package com.example.reservation_solution.dto;

import com.example.reservation_solution.domain.Event;
import com.example.reservation_solution.domain.EventImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private String location;
    private List<String> images;
    private String description;
    private String eventCode;
    private Boolean isPublic;
    private List<ScheduleResponse> schedules;
    private List<QuestionResponse> questions;

    public static EventResponse from(Event event) {
        List<String> imageUrls = event.getImages().stream()
                .sorted(Comparator.comparing(EventImage::getOrderIndex))
                .map(EventImage::getImageUrl)
                .collect(Collectors.toList());

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getLocation(),
                imageUrls,
                event.getDescription(),
                event.getEventCode(),
                event.isPublic(),
                event.getSchedules().stream()
                        .map(ScheduleResponse::from)
                        .collect(Collectors.toList()),
                event.getQuestions().stream()
                        .map(QuestionResponse::from)
                        .collect(Collectors.toList())
        );
    }
}
