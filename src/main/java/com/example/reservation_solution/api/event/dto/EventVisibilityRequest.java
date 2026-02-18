package com.example.reservation_solution.api.event.dto;

import jakarta.validation.constraints.NotNull;

public record EventVisibilityRequest(
    @NotNull(message = "isPublic 값은 필수입니다.")
    Boolean isPublic
) {
}
