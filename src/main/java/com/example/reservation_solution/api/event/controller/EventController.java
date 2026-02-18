package com.example.reservation_solution.api.event.controller;

import com.example.reservation_solution.api.event.dto.CreateEventRequest;
import com.example.reservation_solution.api.event.dto.EventResponse;
import com.example.reservation_solution.api.event.dto.EventUpdateRequest;
import com.example.reservation_solution.api.event.dto.EventVisibilityRequest;
import com.example.reservation_solution.global.docs.*;
import com.example.reservation_solution.global.security.HostUserDetails;
import com.example.reservation_solution.api.event.service.EventService;
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

    @CreateEventDocs
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        EventResponse response = eventService.createEvent(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMyEventsDocs
    @GetMapping
    public ResponseEntity<List<EventResponse>> getMyEvents(@AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        List<EventResponse> events = eventService.getMyEvents(email);
        return ResponseEntity.ok(events);
    }

    @GetEventDetailByHostDocs
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventDetail(
            @PathVariable Long eventId,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        EventResponse response = eventService.getEventDetail(eventId, email);
        return ResponseEntity.ok(response);
    }

    @UpdateEventDocs
    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        EventResponse response = eventService.updateEvent(eventId, email, request);
        return ResponseEntity.ok(response);
    }

    @UpdateEventVisibilityDocs
    @PatchMapping("/{eventId}/visibility")
    public ResponseEntity<Void> updateEventVisibility(
            @PathVariable Long eventId,
            @Valid @RequestBody EventVisibilityRequest request,
            @AuthenticationPrincipal HostUserDetails userDetails) {
        String email = userDetails.getUsername();
        eventService.updateVisibility(eventId, email, request.isPublic());
        return ResponseEntity.ok().build();
    }
}
