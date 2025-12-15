package com.example.reservation_solution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerifyRequest {
    @Schema(description = "인증할 이메일 주소", example = "test@example.com")
    private String email;

    @Schema(description = "전송받은 인증 코드 6자리", example = "123456")
    private String authCode;
}
