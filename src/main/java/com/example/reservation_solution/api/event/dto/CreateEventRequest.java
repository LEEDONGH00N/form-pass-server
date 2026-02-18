package com.example.reservation_solution.api.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "행사명은 필수입니다")
    private String title;

    @NotBlank(message = "장소는 필수입니다")
    private String location;

    private List<String> images;

    private String description;

    @Valid
    @NotEmpty(message = "최소 하나의 일정이 필요합니다")
    private List<ScheduleRequest> schedules;

    @Valid
    private List<QuestionRequest> questions;
}
