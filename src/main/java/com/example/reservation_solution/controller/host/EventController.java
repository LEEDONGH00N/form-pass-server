package com.example.reservation_solution.controller.host;

import com.example.reservation_solution.dto.CreateEventRequest;
import com.example.reservation_solution.dto.EventResponse;
import com.example.reservation_solution.global.security.HostUserDetails;
import com.example.reservation_solution.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Event Host", description = "호스트 이벤트 관리 API")
@RestController
@RequestMapping("/api/host/events")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "이벤트 생성", description = "새로운 이벤트를 생성합니다. (호스트 전용)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이벤트 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        EventResponse response = eventService.createEvent(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "내 이벤트 목록 조회", description = "로그인한 호스트가 생성한 모든 이벤트를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public ResponseEntity<List<EventResponse>> getMyEvents(@AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        List<EventResponse> events = eventService.getMyEvents(email);
        return ResponseEntity.ok(events);
    }

    @Operation(summary = "이벤트 상세 조회", description = "특정 이벤트의 상세 정보를 조회합니다. (본인 이벤트만 조회 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음")
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventDetail(
            @PathVariable Long eventId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        EventResponse response = eventService.getEventDetail(eventId, email);
        return ResponseEntity.ok(response);
    }
}
