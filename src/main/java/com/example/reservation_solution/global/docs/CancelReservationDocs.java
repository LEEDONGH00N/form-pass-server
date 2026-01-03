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
    summary = "예약 취소",
    description = "게스트가 자신의 예약을 취소합니다. (인증 불필요)"
)
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "취소 성공"),
    @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음"),
    @ApiResponse(responseCode = "400", description = "이미 취소된 예약")
})
public @interface CancelReservationDocs {
}
