package com.example.reservation_solution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventVisibilityRequest {
    @NotNull(message = "isPublic 값은 필수입니다.")
    private Boolean isPublic;
}
