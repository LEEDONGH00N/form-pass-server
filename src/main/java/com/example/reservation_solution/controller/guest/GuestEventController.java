package com.example.reservation_solution.controller.guest;

import com.example.reservation_solution.dto.EventResponse;
import com.example.reservation_solution.global.docs.GetAllEventsDocs;
import com.example.reservation_solution.global.docs.GetEventByCodeDocs;
import com.example.reservation_solution.global.security.HostUserDetails;
import com.example.reservation_solution.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Event Guest", description = "게스트 이벤트 조회 API (Public)")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class GuestEventController {

    private final EventService eventService;

    @GetAllEventsDocs
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<EventResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @GetEventByCodeDocs
    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable String eventCode,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        // 선택적 인증: 토큰이 있으면 이메일을 가져오고, 없으면 null
        String requestEmail = (userDetails != null) ? userDetails.getUsername() : null;
        EventResponse response = eventService.getEvent(eventCode, requestEmail);
        return ResponseEntity.ok(response);
    }
}
