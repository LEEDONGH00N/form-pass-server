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
    summary = "예약 상세 조회",
    description = "특정 예약의 상세 정보와 설문 답변을 조회합니다. N+1 문제 방지를 위해 JOIN FETCH를 사용합니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "권한 없음"),
    @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음")
})
@SecurityRequirement(name = "Bearer Auth")
public @interface GetReservationDetailDocs {
}
