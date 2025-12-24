package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.Event;
import com.example.reservation_solution.domain.EventImage;
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
import java.util.stream.IntStream;

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
                .description(request.getDescription())
                .eventCode(eventCode)
                .build();

        if (request.getImages() != null) {
            IntStream.range(0, request.getImages().size())
                    .mapToObj(i -> EventImage.create(request.getImages().get(i), i))
                    .forEach(event::addImage);
        }

        request.getSchedules().stream()
                .map(scheduleReq -> EventSchedule.create(
                        scheduleReq.getStartTime(),
                        scheduleReq.getEndTime(),
                        scheduleReq.getMaxCapacity()))
                .forEach(event::addSchedule);

        if (request.getQuestions() != null) {
            request.getQuestions().stream()
                    .map(questionReq -> FormQuestion.create(
                            questionReq.getQuestionText(),
                            questionReq.getQuestionType(),
                            questionReq.getIsRequired()))
                    .forEach(event::addQuestion);
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
                .toList();
    }

    public EventResponse getEventDetail(Long eventId, String email) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다"));

        if (!event.getHost().getEmail().equals(email)) {
            throw new IllegalArgumentException("해당 이벤트에 대한 권한이 없습니다");
        }
        return EventResponse.from(event);
    }

    public List<EventResponse> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        return events.stream()
                .map(EventResponse::from)
                .toList();
    }

    public EventResponse getEvent(String eventCode, String requestEmail) {
        Event event = eventRepository.findByEventCodeWithDetails(eventCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다"));

        if (!event.getIsPublic()) {
            boolean isOwner = requestEmail != null && event.getHost().getEmail().equals(requestEmail);
            if (!isOwner) {
                throw new AccessDeniedException("비공개된 이벤트입니다");
            }
        }

        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, String email, EventUpdateRequest request) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다"));

        if (!event.getHost().getEmail().equals(email)) {
            throw new IllegalArgumentException("해당 이벤트에 대한 권한이 없습니다");
        }

        event.updateBasicInfo(
                request.getTitle(),
                request.getLocation(),
                request.getDescription()
        );

        event.clearImages();
        if (request.getImages() != null) {
            IntStream.range(0, request.getImages().size())
                    .mapToObj(i -> EventImage.create(request.getImages().get(i), i))
                    .forEach(event::addImage);
        }

        event.clearSchedules();
        request.getSchedules().stream()
                .map(scheduleReq -> EventSchedule.create(
                        scheduleReq.getStartTime(),
                        scheduleReq.getEndTime(),
                        scheduleReq.getMaxCapacity()))
                .forEach(event::addSchedule);

        event.clearQuestions();
        if (request.getQuestions() != null) {
            request.getQuestions().stream()
                    .map(questionReq -> FormQuestion.create(
                            questionReq.getQuestionText(),
                            questionReq.getQuestionType(),
                            questionReq.getIsRequired()))
                    .forEach(event::addQuestion);
        }

        return EventResponse.from(event);
    }

    @Transactional
    public void updateVisibility(Long eventId, String email, Boolean isPublic) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다"));

        if (!event.getHost().getEmail().equals(email)) {
            throw new IllegalArgumentException("해당 이벤트에 대한 권한이 없습니다");
        }

        event.updateVisibility(isPublic);
    }
}
