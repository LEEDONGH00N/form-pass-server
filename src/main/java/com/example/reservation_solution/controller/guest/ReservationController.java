package com.example.reservation_solution.controller.guest;

import com.example.reservation_solution.dto.ReservationLookupRequest;
import com.example.reservation_solution.dto.ReservationLookupResponse;
import com.example.reservation_solution.dto.ReservationRequest;
import com.example.reservation_solution.dto.ReservationResponse;
import com.example.reservation_solution.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reservation Guest", description = "게스트 예약 관리 API (Public)")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "예약 생성", description = "이벤트 스케줄에 예약을 생성합니다. 동시성 제어가 적용되어 정원 초과를 방지합니다. (인증 불필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "예약 생성 성공, QR 토큰 반환"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복 예약, 정원 초과 등)"),
            @ApiResponse(responseCode = "404", description = "스케줄을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "예약 조회", description = "예약 ID로 예약 정보를 조회합니다. (인증 불필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservation(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "QR 토큰으로 예약 조회", description = "QR 토큰을 이용해 예약 정보를 조회합니다. (인증 불필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음")
    })
    @GetMapping("/qr/{qrToken}")
    public ResponseEntity<ReservationResponse> getReservationByQrToken(@PathVariable String qrToken) {
        ReservationResponse response = reservationService.getReservationByQrToken(qrToken);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "예약 취소", description = "게스트가 자신의 예약을 취소합니다. (인증 불필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "이미 취소된 예약")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "예약 내역 조회", description = "이름과 전화번호로 예약 내역을 조회합니다. 결과가 없으면 빈 배열을 반환합니다. (인증 불필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (결과 없으면 빈 배열)")
    })
    @PostMapping("/lookup")
    public ResponseEntity<List<ReservationLookupResponse>> lookupReservations(@RequestBody ReservationLookupRequest request) {
        List<ReservationLookupResponse> responses = reservationService.lookupReservations(
                request.getGuestName(),
                request.getGuestPhoneNumber()
        );
        return ResponseEntity.ok(responses);
    }
}
