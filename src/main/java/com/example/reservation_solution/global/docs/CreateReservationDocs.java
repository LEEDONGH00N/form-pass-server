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
    summary = "예약 생성",
    description = "이벤트 스케줄에 예약을 생성합니다. 동시성 제어가 적용되어 정원 초과를 방지합니다. (인증 불필요)"
)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "예약 생성 성공, QR 토큰 반환"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청\n\n" +
            "- 이미 해당 스케줄에 예약하셨습니다 (중복 예약)\n" +
            "- 이미 매진된 스케줄입니다 (정원 초과)\n" +
            "- 잔여 좌석이 부족합니다 (정원 초과)\n" +
            "- 필수 질문에 답변해야 합니다"),
    @ApiResponse(responseCode = "404", description = "스케줄을 찾을 수 없음")
})
public @interface CreateReservationDocs {
}
