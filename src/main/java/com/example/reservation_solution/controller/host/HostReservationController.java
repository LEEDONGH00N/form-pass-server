package com.example.reservation_solution.controller.host;

import com.example.reservation_solution.dto.*;
import com.example.reservation_solution.global.security.HostUserDetails;
import com.example.reservation_solution.service.HostReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reservation Host", description = "호스트 예약 관리 API")
@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
public class HostReservationController {

    private final HostReservationService hostReservationService;

    @Operation(summary = "대시보드 통계 조회", description = "이벤트의 총 좌석, 예약 인원, 예약률, 입장 완료 인원 등의 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음")
    })
    @GetMapping("/events/{eventId}/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @PathVariable Long eventId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        DashboardResponse response = hostReservationService.getDashboardStats(eventId, email);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "예약자 리스트 조회", description = "이벤트의 예약자 목록을 페이지네이션, 정렬, 필터링하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음")
    })
    @GetMapping("/events/{eventId}/reservations")
    public ResponseEntity<Page<ReservationListResponse>> getReservationList(
            @PathVariable Long eventId,
            @Parameter(description = "특정 스케줄로 필터링 (선택사항)") @RequestParam(required = false) Long scheduleId,
            @AuthenticationPrincipal HostUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String email = userDetails.getUsername();
        Page<ReservationListResponse> response = hostReservationService.getReservationList(eventId, scheduleId, email, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "예약 상세 조회", description = "특정 예약의 상세 정보와 설문 답변을 조회합니다. N+1 문제 방지를 위해 JOIN FETCH를 사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음")
    })
    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservationDetail(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        ReservationResponse response = hostReservationService.getReservationDetail(reservationId, email);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "예약 강제 취소", description = "호스트가 특정 예약을 강제로 취소합니다. 재고가 복구됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "이미 취소된 예약")
    })
    @PatchMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        hostReservationService.cancelReservation(reservationId, email);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "QR 체크인", description = "QR 토큰으로 예약을 확인하고 입장 처리합니다. 이미 입장했거나 취소된 경우 409 Conflict 반환.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입장 완료"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "QR 토큰을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 입장 완료 또는 취소된 예약")
    })
    @PostMapping("/checkin")
    public ResponseEntity<CheckinResponse> checkin(
            @RequestBody CheckinRequest request,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        CheckinResponse response = hostReservationService.checkin(request, email);
        return ResponseEntity.ok(response);
    }
}
