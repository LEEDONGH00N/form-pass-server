package com.example.reservation_solution.global.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
    summary = "인증 메일 전송",
    description = "입력한 이메일로 6자리 인증 코드를 전송합니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "인증 메일 발송 성공"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
})
public @interface SendEmailAuthDocs {
}
