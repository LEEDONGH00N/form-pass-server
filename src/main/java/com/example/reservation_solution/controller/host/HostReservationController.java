package com.example.reservation_solution.controller.host;

import com.example.reservation_solution.dto.*;
import com.example.reservation_solution.global.docs.*;
import com.example.reservation_solution.global.security.HostUserDetails;
import com.example.reservation_solution.service.HostReservationService;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

@Tag(name = "Reservation Host", description = "호스트 예약 관리 API")
@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
public class HostReservationController {

    private final HostReservationService hostReservationService;

    @GetDashboardDocs
    @GetMapping("/events/{eventId}/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @PathVariable Long eventId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        DashboardResponse response = hostReservationService.getDashboardStats(eventId, email);
        return ResponseEntity.ok(response);
    }

    @GetReservationListDocs
    @GetMapping("/events/{eventId}/reservations")
    public ResponseEntity<Page<ReservationListResponse>> getReservationList(
            @PathVariable Long eventId,
            @Parameter(description = "특정 스케줄로 필터링 (선택사항)") @RequestParam(required = false) Long scheduleId,
            @Parameter(description = "이름 또는 전화번호로 검색 (선택사항)") @RequestParam(required = false) String searchKeyword,
            @AuthenticationPrincipal HostUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String email = userDetails.getUsername();
        Page<ReservationListResponse> response = hostReservationService.getReservationList(eventId, scheduleId, searchKeyword, email, pageable);
        return ResponseEntity.ok(response);
    }

    @GetReservationDetailDocs
    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservationDetail(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        ReservationResponse response = hostReservationService.getReservationDetail(reservationId, email);
        return ResponseEntity.ok(response);
    }

    @CancelReservationByHostDocs
    @PatchMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        hostReservationService.cancelReservation(reservationId, email);
        return ResponseEntity.noContent().build();
    }

    @CheckinByQrDocs
    @PostMapping("/checkin")
    public ResponseEntity<CheckinResponse> checkin(
            @RequestBody CheckinRequest request,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        CheckinResponse response = hostReservationService.checkin(request, email);
        return ResponseEntity.ok(response);
    }

    @ManualCheckinDocs
    @PatchMapping("/reservations/{reservationId}/checkin")
    public ResponseEntity<CheckinResponse> manualCheckin(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        CheckinResponse response = hostReservationService.manualCheckin(reservationId, email);
        return ResponseEntity.ok(response);
    }

    @GetScheduleStatusDocs
    @GetMapping("/events/{eventId}/schedules-status")
    public ResponseEntity<List<ScheduleStatusResponse>> getScheduleStatus(
            @PathVariable Long eventId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ScheduleStatusResponse> response = hostReservationService.getScheduleStatus(eventId, email);
        return ResponseEntity.ok(response);
    }
}
