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
    summary = "이벤트 상세 조회",
    description = "특정 이벤트의 상세 정보를 조회합니다. 비공개 이벤트의 경우 호스트 본인만 조회 가능합니다. (선택적 인증)"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "비공개된 이벤트입니다"),
    @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음")
})
public @interface GetEventByCodeDocs {
}
