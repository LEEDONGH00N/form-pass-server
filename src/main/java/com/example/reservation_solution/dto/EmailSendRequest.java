package com.example.reservation_solution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailSendRequest {
    @Schema(description = "인증할 이메일 주소", example = "test@example.com")
    private String email;
}
