package com.example.reservation_solution.global.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
    summary = "이벤트 생성",
    description = "새로운 이벤트를 생성합니다. (호스트 전용)"
)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "이벤트 생성 성공"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
    @ApiResponse(responseCode = "403", description = "권한 없음")
})
@SecurityRequirement(name = "Bearer Auth")
public @interface CreateEventDocs {
}
