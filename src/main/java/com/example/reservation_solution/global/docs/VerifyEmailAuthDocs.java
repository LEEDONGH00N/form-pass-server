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
    summary = "인증 코드 검증",
    description = "이메일과 인증 코드를 입력받아 유효성을 검사합니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "인증 성공"),
    @ApiResponse(responseCode = "400", description = "인증 코드가 올바르지 않거나 만료됨")
})
public @interface VerifyEmailAuthDocs {
}
