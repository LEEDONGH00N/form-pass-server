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
    summary = "모든 이벤트 조회",
    description = "등록된 모든 이벤트 목록을 조회합니다. (인증 불필요)"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공")
})
public @interface GetAllEventsDocs {
}
