package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.Event;
import com.example.reservation_solution.domain.EventSchedule;
import com.example.reservation_solution.domain.FormQuestion;
import com.example.reservation_solution.domain.Host;
import com.example.reservation_solution.dto.CreateEventRequest;
import com.example.reservation_solution.dto.EventResponse;
import com.example.reservation_solution.dto.EventUpdateRequest;
import com.example.reservation_solution.global.util.CodeGenerator;
import com.example.reservation_solution.repository.EventRepository;
import com.example.reservation_solution.repository.HostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final HostRepository hostRepository;

    @Transactional
    public EventResponse createEvent(String email, CreateEventRequest request) {
        Host host = hostRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 호스트입니다"));

        String eventCode;
        do {
            eventCode = CodeGenerator.generateEventCode();
        } while (eventRepository.existsByEventCode(eventCode));

        Event event = Event.builder()
                .host(host)
                .title(request.getTitle())
                .location(request.getLocation())
                .thumbnailUrl(request.getThumbnailUrl())
                .description(request.getDescription())
                .eventCode(eventCode)
                .build();

        request.getSchedules().forEach(scheduleReq -> {
            EventSchedule schedule = EventSchedule.builder()
                    .startTime(scheduleReq.getStartTime())
                    .endTime(scheduleReq.getEndTime())
                    .maxCapacity(scheduleReq.getMaxCapacity())
                    .build();
            event.addSchedule(schedule);
        });

        if (request.getQuestions() != null) {
            request.getQuestions().forEach(questionReq -> {
                FormQuestion question = FormQuestion.builder()
                        .questionText(questionReq.getQuestionText())
                        .questionType(questionReq.getQuestionType())
                        .isRequired(questionReq.getIsRequired())
                        .build();
                event.addQuestion(question);
            });
        }

        Event savedEvent = eventRepository.save(event);
        return EventResponse.from(savedEvent);
    }

    public List<EventResponse> getMyEvents(String email) {
        Host host = hostRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 호스트입니다"));

        List<Event> events = eventRepository.findByHost(host);
        return events.stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
    }

    public EventResponse getEventDetail(Long eventId, String email) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다"));

        if (!event.getHost().getEmail().equals(email)) {
            throw new IllegalArgumentException("해당 이벤트에 대한 권한이 없습니다");
        }
        return EventResponse.from(event);
    }

    // Guest API methods
    public List<EventResponse> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        return events.stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
    }

    public EventResponse getEvent(String eventCode, String requestEmail) {
        Event event = eventRepository.findByEventCodeWithDetails(eventCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다"));

        // 공개 여부 체크 (Security Check)
        if (!event.getIsPublic()) {
            // 비공개 이벤트의 경우
            boolean isOwner = requestEmail != null && event.getHost().getEmail().equals(requestEmail);
            if (!isOwner) {
                // Case B: 호스트 본인이 아니면 403 예외 발생
                throw new AccessDeniedException("비공개된 이벤트입니다");
            }
            // Case A: 호스트 본인이면 정상 반환 (미리보기 기능)
        }

        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, String email, EventUpdateRequest request) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다"));

        // 권한 확인
        if (!event.getHost().getEmail().equals(email)) {
            throw new IllegalArgumentException("해당 이벤트에 대한 권한이 없습니다");
        }

        // 기본 정보 업데이트 (Dirty Checking)
        event.updateBasicInfo(
                request.getTitle(),
                request.getLocation(),
                request.getThumbnailUrl(),
                request.getDescription()
        );

        // 기존 스케줄 삭제 및 새 스케줄 추가
        event.clearSchedules();
        request.getSchedules().forEach(scheduleReq -> {
            EventSchedule schedule = EventSchedule.builder()
                    .startTime(scheduleReq.getStartTime())
                    .endTime(scheduleReq.getEndTime())
                    .maxCapacity(scheduleReq.getMaxCapacity())
                    .build();
            event.addSchedule(schedule);
        });

        // 기존 질문 삭제 및 새 질문 추가
        event.clearQuestions();
        if (request.getQuestions() != null) {
            request.getQuestions().forEach(questionReq -> {
                FormQuestion question = FormQuestion.builder()
                        .questionText(questionReq.getQuestionText())
                        .questionType(questionReq.getQuestionType())
                        .isRequired(questionReq.getIsRequired())
                        .build();
                event.addQuestion(question);
            });
        }

        return EventResponse.from(event);
    }

    @Transactional
    public void updateVisibility(Long eventId, String email, Boolean isPublic) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다"));

        // 권한 확인: 현재 로그인한 사용자가 해당 이벤트의 호스트인지 확인
        if (!event.getHost().getEmail().equals(email)) {
            throw new IllegalArgumentException("해당 이벤트에 대한 권한이 없습니다");
        }

        // Dirty Checking을 이용한 상태 업데이트
        event.updateVisibility(isPublic);
    }
}
