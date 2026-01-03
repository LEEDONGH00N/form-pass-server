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
    summary = "스케줄별 예약 현황 조회",
    description = "이벤트의 모든 스케줄과 각 스케줄별 예약자 리스트를 한 번에 조회합니다. (대시보드 칸반 보드용)"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음")
})
@SecurityRequirement(name = "Bearer Auth")
public @interface GetScheduleStatusDocs {
}
