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
    summary = "수동 체크인",
    description = "예약 ID로 수동 입장 처리합니다. QR 스캔이 안 될 때 사용합니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "입장 완료"),
    @ApiResponse(responseCode = "403", description = "권한 없음"),
    @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음"),
    @ApiResponse(responseCode = "409", description = "이미 입장 완료 또는 취소된 예약")
})
@SecurityRequirement(name = "Bearer Auth")
public @interface ManualCheckinDocs {
}
