package com.example.reservation_solution.support;

import com.example.reservation_solution.api.auth.domain.Host;
import com.example.reservation_solution.api.auth.domain.Role;
import com.example.reservation_solution.api.event.domain.*;
import com.example.reservation_solution.api.reservation.domain.FormAnswer;
import com.example.reservation_solution.api.reservation.domain.Reservation;
import com.example.reservation_solution.api.reservation.dto.FormAnswerRequest;
import com.example.reservation_solution.api.reservation.dto.ReservationRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

public class TestFixtures {

    public static Host createHost() {
        return createHost("host@example.com");
    }

    public static Host createHost(String email) {
        Host host = Host.create(email, "encodedPassword", "테스트호스트", Role.HOST);
        ReflectionTestUtils.setField(host, "id", 1L);
        return host;
    }

    public static Event createEvent(Host host) {
        Event event = Event.builder()
                .host(host)
                .title("테스트 이벤트")
                .location("서울시 강남구")
                .description("테스트 설명")
                .eventCode("EVT12345")
                .build();
        ReflectionTestUtils.setField(event, "id", 1L);
        return event;
    }

    public static EventSchedule createEventSchedule(int maxCapacity) {
        EventSchedule schedule = EventSchedule.create(
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 4, 1, 12, 0),
                maxCapacity
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        return schedule;
    }

    public static EventSchedule createEventScheduleWithEvent(Host host, int maxCapacity) {
        Event event = createEvent(host);
        EventSchedule schedule = createEventSchedule(maxCapacity);
        event.addSchedule(schedule);
        return schedule;
    }

    public static FormQuestion createFormQuestion(Event event, boolean isRequired) {
        FormQuestion question = FormQuestion.create("추가 질문", QuestionType.TEXT, isRequired);
        ReflectionTestUtils.setField(question, "id", 100L);
        event.addQuestion(question);
        return question;
    }

    public static Reservation createReservation(EventSchedule schedule) {
        Reservation reservation = Reservation.create(schedule, "홍길동", "encryptedPhone", 1);
        ReflectionTestUtils.setField(reservation, "id", 1L);
        return reservation;
    }

    public static Reservation createReservation(EventSchedule schedule, int ticketCount) {
        Reservation reservation = Reservation.create(schedule, "홍길동", "encryptedPhone", ticketCount);
        ReflectionTestUtils.setField(reservation, "id", 1L);
        return reservation;
    }

    public static ReservationRequest createReservationRequest(Long scheduleId) {
        return new ReservationRequest(
                scheduleId,
                "홍길동",
                "01012345678",
                1,
                null
        );
    }

    public static ReservationRequest createReservationRequestWithAnswers(Long scheduleId, Long questionId) {
        return new ReservationRequest(
                scheduleId,
                "홍길동",
                "01012345678",
                1,
                List.of(new FormAnswerRequest(questionId, "답변 내용"))
        );
    }
}
