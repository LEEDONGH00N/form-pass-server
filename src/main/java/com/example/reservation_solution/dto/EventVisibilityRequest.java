package com.example.reservation_solution.dto;

import jakarta.validation.constraints.NotNull;

public record EventVisibilityRequest(
    @NotNull(message = "isPublic 값은 필수입니다.")
    Boolean isPublic
) {
}
