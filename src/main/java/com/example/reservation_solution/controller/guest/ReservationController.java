package com.example.reservation_solution.controller.guest;

import com.example.reservation_solution.dto.ReservationLookupRequest;
import com.example.reservation_solution.dto.ReservationLookupResponse;
import com.example.reservation_solution.dto.ReservationRequest;
import com.example.reservation_solution.dto.ReservationResponse;
import com.example.reservation_solution.global.docs.*;
import com.example.reservation_solution.service.ReservationService;
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

    @CreateReservationDocs
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetReservationDocs
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservation(id);
        return ResponseEntity.ok(response);
    }

    @GetReservationByQrTokenDocs
    @GetMapping("/qr/{qrToken}")
    public ResponseEntity<ReservationResponse> getReservationByQrToken(@PathVariable String qrToken) {
        ReservationResponse response = reservationService.getReservationByQrToken(qrToken);
        return ResponseEntity.ok(response);
    }

    @CancelReservationDocs
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    @LookupReservationsDocs
    @PostMapping("/lookup")
    public ResponseEntity<List<ReservationLookupResponse>> lookupReservations(@RequestBody ReservationLookupRequest request) {
        List<ReservationLookupResponse> responses = reservationService.lookupReservations(
                request.getGuestName(),
                request.getGuestPhoneNumber()
        );
        return ResponseEntity.ok(responses);
    }
}
