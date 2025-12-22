package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.*;
import com.example.reservation_solution.dto.FormAnswerRequest;
import com.example.reservation_solution.dto.ReservationRequest;
import com.example.reservation_solution.dto.ReservationResponse;
import com.example.reservation_solution.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventScheduleRepository eventScheduleRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private FormQuestionRepository formQuestionRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Host host;
    private Event event;
    private EventSchedule schedule;
    private FormQuestion requiredQuestion;
    private FormQuestion optionalQuestion;

    @BeforeEach
    void setUp() {
        // 호스트 생성
        host = Host.builder()
                .email("test@example.com")
                .password("password")
                .name("테스트 호스트")
                .role(Role.HOST)
                .build();
        hostRepository.save(host);

        // 이벤트 생성
        event = Event.builder()
                .host(host)
                .title("테스트 이벤트")
                .description("테스트 설명")
                .location("테스트 장소")
                .eventCode("TEST001")
                .build();
        event.updateVisibility(true);
        eventRepository.save(event);

        // 스케줄 생성 (정원 10명)
        schedule = EventSchedule.builder()
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .maxCapacity(10)
                .build();
        event.addSchedule(schedule);
        eventScheduleRepository.save(schedule);

        // 필수 질문 생성
        requiredQuestion = FormQuestion.builder()
                .questionText("필수 질문입니다")
                .questionType(QuestionType.TEXT)
                .isRequired(true)
                .build();
        event.addQuestion(requiredQuestion);
        formQuestionRepository.save(requiredQuestion);

        // 선택 질문 생성
        optionalQuestion = FormQuestion.builder()
                .questionText("선택 질문입니다")
                .questionType(QuestionType.TEXT)
                .isRequired(false)
                .build();
        event.addQuestion(optionalQuestion);
        formQuestionRepository.save(optionalQuestion);
    }

    @Test
    @DisplayName("정상적인 예약 생성")
    void createReservation_Success() {
        // given
        List<FormAnswerRequest> answers = List.of(
                new FormAnswerRequest(requiredQuestion.getId(), "필수 답변")
        );

        ReservationRequest request = new ReservationRequest(
                schedule.getId(),
                "홍길동",
                "01012345678",
                2,
                answers
        );

        // when
        ReservationResponse response = reservationService.createReservation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getQrToken()).isNotNull();
        assertThat(response.getGuestName()).isEqualTo("홍길동");
        assertThat(response.getTicketCount()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        // 스케줄의 예약 카운트 확인
        EventSchedule updatedSchedule = eventScheduleRepository.findById(schedule.getId()).get();
        assertThat(updatedSchedule.getReservedCount()).isEqualTo(2);
        assertThat(updatedSchedule.getAvailableSeats()).isEqualTo(8);
    }

    @Test
    @DisplayName("이름과 전화번호 없이도 예약 가능")
    void createReservation_WithoutNameAndPhone() {
        // given
        List<FormAnswerRequest> answers = List.of(
                new FormAnswerRequest(requiredQuestion.getId(), "필수 답변")
        );

        ReservationRequest request = new ReservationRequest(
                schedule.getId(),
                null,
                null,
                1,
                answers
        );

        // when
        ReservationResponse response = reservationService.createReservation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getQrToken()).isNotNull();
        assertThat(response.getTicketCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("중복 예약 방지 - 같은 전화번호로 동일 스케줄에 재예약 불가")
    void createReservation_DuplicatePhoneNumber() {
        // given
        List<FormAnswerRequest> answers = List.of(
                new FormAnswerRequest(requiredQuestion.getId(), "필수 답변")
        );

        ReservationRequest firstRequest = new ReservationRequest(
                schedule.getId(),
                "홍길동",
                "01012345678",
                1,
                answers
        );

        // 첫 번째 예약 성공
        reservationService.createReservation(firstRequest);

        // when & then - 같은 전화번호로 재예약 시도
        ReservationRequest secondRequest = new ReservationRequest(
                schedule.getId(),
                "김철수",
                "01012345678",
                1,
                answers
        );

        assertThatThrownBy(() -> reservationService.createReservation(secondRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 해당 스케줄에 예약하셨습니다");
    }

    @Test
    @DisplayName("정원 초과 시 예약 불가 - 매진")
    void createReservation_SoldOut() {
        // given - 정원이 10명인데 이미 10명 예약됨
        for (int i = 0; i < 10; i++) {
            schedule.incrementReservedCount();
        }
        eventScheduleRepository.save(schedule);

        List<FormAnswerRequest> answers = List.of(
                new FormAnswerRequest(requiredQuestion.getId(), "필수 답변")
        );

        ReservationRequest request = new ReservationRequest(
                schedule.getId(),
                "홍길동",
                "01012345678",
                1,
                answers
        );

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 매진된 스케줄입니다");
    }

    @Test
    @DisplayName("정원 초과 시 예약 불가 - 잔여 좌석 부족")
    void createReservation_InsufficientSeats() {
        // given - 정원이 10명인데 이미 8명 예약됨
        for (int i = 0; i < 8; i++) {
            schedule.incrementReservedCount();
        }
        eventScheduleRepository.save(schedule);

        // 3명을 예약하려고 시도 (잔여 좌석 2명)
        List<FormAnswerRequest> answers = List.of(
                new FormAnswerRequest(requiredQuestion.getId(), "필수 답변")
        );

        ReservationRequest request = new ReservationRequest(
                schedule.getId(),
                "홍길동",
                "01012345678",
                3,
                answers
        );

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔여 좌석이 부족합니다");
    }

    @Test
    @DisplayName("필수 질문에 답변하지 않으면 예약 불가")
    void createReservation_RequiredQuestionNotAnswered() {
        // given - 필수 질문에 답변하지 않음
        ReservationRequest request = new ReservationRequest(
                schedule.getId(),
                "홍길동",
                "01012345678",
                1,
                new ArrayList<>() // 빈 답변 리스트
        );

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("필수 질문에 답변해야 합니다");
    }

    @Test
    @DisplayName("필수 질문에 빈 답변을 제출하면 예약 불가")
    void createReservation_RequiredQuestionEmptyAnswer() {
        // given - 필수 질문에 빈 답변
        List<FormAnswerRequest> answers = List.of(
                new FormAnswerRequest(requiredQuestion.getId(), "  ") // 공백만 있는 답변
        );

        ReservationRequest request = new ReservationRequest(
                schedule.getId(),
                "홍길동",
                "01012345678",
                1,
                answers
        );

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("필수 질문에 답변해야 합니다");
    }

    @Test
    @DisplayName("선택 질문은 답변하지 않아도 예약 가능")
    void createReservation_OptionalQuestionNotAnswered() {
        // given - 필수 질문만 답변
        List<FormAnswerRequest> answers = List.of(
                new FormAnswerRequest(requiredQuestion.getId(), "필수 답변")
        );

        ReservationRequest request = new ReservationRequest(
                schedule.getId(),
                "홍길동",
                "01012345678",
                1,
                answers
        );

        // when
        ReservationResponse response = reservationService.createReservation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getQrToken()).isNotNull();
    }

    @Test
    @DisplayName("ticketCount가 null이면 기본값 1로 설정")
    void createReservation_NullTicketCount() {
        // given
        List<FormAnswerRequest> answers = List.of(
                new FormAnswerRequest(requiredQuestion.getId(), "필수 답변")
        );

        ReservationRequest request = new ReservationRequest(
                schedule.getId(),
                "홍길동",
                "01012345678",
                null,
                answers
        );

        // when
        ReservationResponse response = reservationService.createReservation(request);

        // then
        assertThat(response.getTicketCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("ticketCount가 0 이하면 기본값 1로 설정")
    void createReservation_ZeroTicketCount() {
        // given
        List<FormAnswerRequest> answers = List.of(
                new FormAnswerRequest(requiredQuestion.getId(), "필수 답변")
        );

        ReservationRequest request = new ReservationRequest(
                schedule.getId(),
                "홍길동",
                "01012345678",
                0,
                answers
        );

        // when
        ReservationResponse response = reservationService.createReservation(request);

        // then
        assertThat(response.getTicketCount()).isEqualTo(1);
    }
}
