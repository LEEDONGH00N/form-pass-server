package com.example.reservation_solution.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailSendRequest(
    @Schema(description = "인증할 이메일 주소", example = "test@example.com")
    String email
) {
}
