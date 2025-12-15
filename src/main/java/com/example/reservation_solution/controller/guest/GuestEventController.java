package com.example.reservation_solution.controller.guest;

import com.example.reservation_solution.dto.EventResponse;
import com.example.reservation_solution.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Event Guest", description = "게스트 이벤트 조회 API (Public)")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class GuestEventController {

    private final EventService eventService;

    @Operation(summary = "모든 이벤트 조회", description = "등록된 모든 이벤트 목록을 조회합니다. (인증 불필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<EventResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @Operation(summary = "이벤트 상세 조회", description = "특정 이벤트의 상세 정보를 조회합니다. 잔여 좌석 정보가 포함됩니다. (인증 불필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음")
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        EventResponse response = eventService.getEvent(eventId);
        return ResponseEntity.ok(response);
    }
}
