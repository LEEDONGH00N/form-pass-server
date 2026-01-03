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
    summary = "예약 강제 취소",
    description = "호스트가 특정 예약을 강제로 취소합니다. 재고가 복구됩니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "취소 성공"),
    @ApiResponse(responseCode = "403", description = "권한 없음"),
    @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음"),
    @ApiResponse(responseCode = "400", description = "이미 취소된 예약")
})
@SecurityRequirement(name = "Bearer Auth")
public @interface CancelReservationByHostDocs {
}
