package com.example.reservation_solution.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailVerifyRequest(
    @Schema(description = "인증할 이메일 주소", example = "test@example.com")
    String email,

    @Schema(description = "전송받은 인증 코드 6자리", example = "123456")
    String authCode
) {
}
