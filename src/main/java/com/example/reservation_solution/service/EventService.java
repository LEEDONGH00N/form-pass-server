package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.Event;
import com.example.reservation_solution.domain.EventSchedule;
import com.example.reservation_solution.domain.FormQuestion;
import com.example.reservation_solution.domain.Host;
import com.example.reservation_solution.dto.CreateEventRequest;
import com.example.reservation_solution.dto.EventResponse;
import com.example.reservation_solution.repository.EventRepository;
import com.example.reservation_solution.repository.HostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        Event event = Event.builder()
                .host(host)
                .title(request.getTitle())
                .location(request.getLocation())
                .thumbnailUrl(request.getThumbnailUrl())
                .description(request.getDescription())
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

    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다"));
        return EventResponse.from(event);
    }
}
