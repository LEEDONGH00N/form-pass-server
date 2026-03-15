package com.example.reservation_solution.api.reservation.repository;

import com.example.reservation_solution.api.auth.domain.Host;
import com.example.reservation_solution.api.auth.domain.Role;
import com.example.reservation_solution.api.auth.repository.HostRepository;
import com.example.reservation_solution.api.event.domain.Event;
import com.example.reservation_solution.api.event.domain.EventSchedule;
import com.example.reservation_solution.api.event.domain.FormQuestion;
import com.example.reservation_solution.api.event.domain.QuestionType;
import com.example.reservation_solution.api.event.repository.EventRepository;
import com.example.reservation_solution.api.reservation.domain.FormAnswer;
import com.example.reservation_solution.api.reservation.domain.Reservation;
import com.example.reservation_solution.api.reservation.domain.ReservationStatus;
import com.example.reservation_solution.global.config.JpaAuditingConfig;
import com.example.reservation_solution.global.config.QuerydslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class ReservationRepositoryImplTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private HostRepository hostRepository;

    private EventSchedule savedSchedule;
    private Event savedEvent;

    @BeforeEach
    void setUp() {
        Host host = Host.create("host@test.com", "password", "테스트호스트", Role.HOST);
        hostRepository.save(host);

        Event event = Event.builder()
                .host(host)
                .title("테스트 이벤트")
                .location("서울")
                .description("설명")
                .eventCode("EVT00001")
                .build();

        EventSchedule schedule = EventSchedule.create(
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 4, 1, 12, 0),
                100
        );
        event.addSchedule(schedule);

        FormQuestion question = FormQuestion.create("추가 질문", QuestionType.TEXT, false);
        event.addQuestion(question);

        savedEvent = eventRepository.save(event);
        savedSchedule = savedEvent.getSchedules().get(0);
    }

    @Test
    @DisplayName("스케줄ID와 전화번호로 예약을 조회한다")
    void findByScheduleIdAndPhoneNumberAndStatus_success() {
        // given
        Reservation reservation = Reservation.create(savedSchedule, "홍길동", "encryptedPhone", 1);
        reservationRepository.save(reservation);

        // when
        Optional<Reservation> result = reservationRepository.findByScheduleIdAndPhoneNumberAndStatus(
                savedSchedule.getId(), "encryptedPhone", ReservationStatus.CONFIRMED);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getGuestName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("예약 상세 조회 시 fetch join으로 연관 엔티티를 함께 로딩한다")
    void findByIdWithDetails_fetchJoin() {
        // given
        Reservation reservation = Reservation.create(savedSchedule, "홍길동", "encryptedPhone", 1);
        FormQuestion question = savedEvent.getQuestions().get(0);
        FormAnswer answer = FormAnswer.create(question, "답변");
        reservation.addFormAnswer(answer);
        reservationRepository.save(reservation);

        // when
        Optional<Reservation> result = reservationRepository.findByIdWithDetails(reservation.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getFormAnswers()).hasSize(1);
        assertThat(result.get().getEventSchedule()).isNotNull();
    }

    @Test
    @DisplayName("게스트 정보로 예약 목록을 조회한다")
    void findByGuestInfoAndStatus_success() {
        // given
        Reservation reservation = Reservation.create(savedSchedule, "홍길동", "encryptedPhone", 1);
        reservationRepository.save(reservation);

        // when
        List<Reservation> results = reservationRepository.findByGuestInfoAndStatus(
                "홍길동", "encryptedPhone", ReservationStatus.CONFIRMED);

        // then
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("키워드로 예약을 검색한다")
    void searchReservations_withKeyword() {
        // given
        Reservation reservation = Reservation.create(savedSchedule, "홍길동", "encryptedPhone", 1);
        reservationRepository.save(reservation);

        // when
        Page<Reservation> results = reservationRepository.searchReservations(
                null, List.of(savedSchedule.getId()), "홍길동", PageRequest.of(0, 10));

        // then
        assertThat(results.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("페이지네이션이 정상 동작한다")
    void searchReservations_pagination() {
        // given
        for (int i = 0; i < 5; i++) {
            Reservation reservation = Reservation.create(savedSchedule, "게스트" + i, "phone" + i, 1);
            reservationRepository.save(reservation);
        }

        // when
        Page<Reservation> results = reservationRepository.searchReservations(
                savedSchedule.getId(), null, null, PageRequest.of(0, 3));

        // then
        assertThat(results.getContent()).hasSize(3);
        assertThat(results.getTotalElements()).isEqualTo(5);
        assertThat(results.getTotalPages()).isEqualTo(2);
    }
}
