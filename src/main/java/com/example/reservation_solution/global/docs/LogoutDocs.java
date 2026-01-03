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
    summary = "로그아웃",
    description = "현재 세션의 JWT 토큰을 무효화하고 쿠키를 삭제합니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
    @ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
})
public @interface LogoutDocs {
}
