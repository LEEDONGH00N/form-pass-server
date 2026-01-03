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
    summary = "QR 체크인",
    description = "QR 토큰으로 예약을 확인하고 입장 처리합니다. 이미 입장했거나 취소된 경우 409 Conflict 반환."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "입장 완료"),
    @ApiResponse(responseCode = "403", description = "권한 없음"),
    @ApiResponse(responseCode = "404", description = "QR 토큰을 찾을 수 없음"),
    @ApiResponse(responseCode = "409", description = "이미 입장 완료 또는 취소된 예약")
})
@SecurityRequirement(name = "Bearer Auth")
public @interface CheckinByQrDocs {
}
