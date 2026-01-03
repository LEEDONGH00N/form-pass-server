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
    summary = "예약 내역 조회",
    description = "이름과 전화번호로 예약 내역을 조회합니다. 결과가 없으면 빈 배열을 반환합니다. (인증 불필요)"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공 (결과 없으면 빈 배열)")
})
public @interface LookupReservationsDocs {
}
