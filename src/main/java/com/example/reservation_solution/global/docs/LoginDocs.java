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
    summary = "로그인",
    description = "호스트 계정으로 로그인하여 JWT 토큰을 HttpOnly 쿠키로 발급받습니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰이 HttpOnly 쿠키로 설정됨"),
    @ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 오류)")
})
public @interface LoginDocs {
}
