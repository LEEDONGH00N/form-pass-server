package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.*;
import com.example.reservation_solution.dto.FormAnswerRequest;
import com.example.reservation_solution.dto.ReservationRequest;
import com.example.reservation_solution.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 생성의 동시성 제어를 검증하는 테스트
 * - @Transactional을 사용하지 않아 실제 DB 커밋이 발생
 * - 비관적 락(Pessimistic Lock)이 제대로 동작하는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class ReservationConcurrencyTest {

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

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        reservationRepository.deleteAll();
        formQuestionRepository.deleteAll();
        eventScheduleRepository.deleteAll();
        eventRepository.deleteAll();
        hostRepository.deleteAll();

        // 호스트 생성
        host = Host.builder()
                .email("concurrency-test@example.com")
                .password("password")
                .name("동시성 테스트 호스트")
                .role(Role.HOST)
                .build();
        hostRepository.saveAndFlush(host);

        // 이벤트 생성
        event = Event.builder()
                .host(host)
                .title("동시성 테스트 이벤트")
                .description("동시성 테스트 설명")
                .location("동시성 테스트 장소")
                .eventCode("CONCUR001")
                .build();
        event.updateVisibility(true);
        eventRepository.saveAndFlush(event);

        // 스케줄 생성 (정원 10명)
        schedule = EventSchedule.builder()
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .maxCapacity(10)
                .build();
        event.addSchedule(schedule);
        eventScheduleRepository.saveAndFlush(schedule);

        // 필수 질문 생성
        requiredQuestion = FormQuestion.builder()
                .questionText("동시성 테스트 필수 질문")
                .questionType(QuestionType.TEXT)
                .isRequired(true)
                .build();
        event.addQuestion(requiredQuestion);
        formQuestionRepository.saveAndFlush(requiredQuestion);
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        reservationRepository.deleteAll();
        formQuestionRepository.deleteAll();
        eventScheduleRepository.deleteAll();
        eventRepository.deleteAll();
        hostRepository.deleteAll();
    }

    @Test
    @DisplayName("동시성 테스트 - 20명이 동시에 1장씩 예약 시도, 정원 10명이면 10명만 성공")
    void concurrentReservation_10Capacity_20Users() throws InterruptedException {
        // given
        int threadCount = 20; // 20명이 동시에 예약 시도
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 20개의 스레드에서 동시에 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    List<FormAnswerRequest> answers = List.of(
                            new FormAnswerRequest(requiredQuestion.getId(), "답변" + userId)
                    );

                    ReservationRequest request = new ReservationRequest(
                            schedule.getId(),
                            "게스트" + userId,
                            "0101234" + String.format("%04d", userId),
                            1,
                            answers
                    );

                    reservationService.createReservation(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 완료될 때까지 대기
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(10); // 정원 10명만 성공
        assertThat(failCount.get()).isEqualTo(10); // 나머지 10명은 실패

        // DB에서 실제 저장된 예약 수 확인
        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(10);

        // 스케줄의 예약 카운트 확인
        EventSchedule updatedSchedule = eventScheduleRepository.findById(schedule.getId()).get();
        assertThat(updatedSchedule.getReservedCount()).isEqualTo(10);
        assertThat(updatedSchedule.getAvailableSeats()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 테스트 - 10명이 동시에 2장씩 예약 시도, 정원 10명이면 5명만 성공")
    void concurrentReservation_10Capacity_10UsersWithTwoTickets() throws InterruptedException {
        // given
        int threadCount = 10; // 10명이 동시에 예약 시도
        int ticketCountPerUser = 2; // 각 사용자는 2장씩 예약
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 10개의 스레드에서 동시에 2장씩 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    List<FormAnswerRequest> answers = List.of(
                            new FormAnswerRequest(requiredQuestion.getId(), "답변" + userId)
                    );

                    ReservationRequest request = new ReservationRequest(
                            schedule.getId(),
                            "게스트" + userId,
                            "0101234" + String.format("%04d", userId),
                            ticketCountPerUser,
                            answers
                    );

                    reservationService.createReservation(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 완료될 때까지 대기
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(5); // 정원 10명 / 2장 = 5명 성공
        assertThat(failCount.get()).isEqualTo(5); // 나머지 5명은 실패

        // DB에서 실제 저장된 예약 수 확인
        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(5);

        // 스케줄의 예약 카운트 확인
        EventSchedule updatedSchedule = eventScheduleRepository.findById(schedule.getId()).get();
        assertThat(updatedSchedule.getReservedCount()).isEqualTo(10);
        assertThat(updatedSchedule.getAvailableSeats()).isEqualTo(0);

        // 각 예약의 티켓 수 확인
        reservations.forEach(reservation ->
            assertThat(reservation.getTicketCount()).isEqualTo(2)
        );
    }

    @Test
    @DisplayName("동시성 테스트 - 15명이 동시에 예약 시도, 정원 10명이면 처음 10명만 성공")
    void concurrentReservation_RaceCondition() throws InterruptedException {
        // given
        int threadCount = 15;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    List<FormAnswerRequest> answers = List.of(
                            new FormAnswerRequest(requiredQuestion.getId(), "답변" + userId)
                    );

                    ReservationRequest request = new ReservationRequest(
                            schedule.getId(),
                            "게스트" + userId,
                            "0109999" + String.format("%04d", userId),
                            1,
                            answers
                    );

                    reservationService.createReservation(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예외 발생 시 무시 (정원 초과 등)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확히 10명만 예약 성공해야 함 (정원 초과 방지)
        assertThat(successCount.get()).isEqualTo(10);

        EventSchedule updatedSchedule = eventScheduleRepository.findById(schedule.getId()).get();
        assertThat(updatedSchedule.getReservedCount()).isEqualTo(10);

        // 실제 DB에 저장된 예약도 10건
        List<Reservation> allReservations = reservationRepository.findAll();
        assertThat(allReservations).hasSize(10);
    }

    @Test
    @DisplayName("동시성 테스트 - 정원이 거의 찬 상태에서 동시 예약 시도")
    void concurrentReservation_AlmostFull() throws InterruptedException {
        // given - 정원 10명 중 8명이 이미 예약됨 (잔여 좌석 2명)
        EventSchedule scheduleFromDb = eventScheduleRepository.findById(schedule.getId()).get();
        for (int i = 0; i < 8; i++) {
            scheduleFromDb.incrementReservedCount();
        }
        eventScheduleRepository.saveAndFlush(scheduleFromDb);

        int threadCount = 5; // 5명이 동시에 1장씩 예약 시도
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    List<FormAnswerRequest> answers = List.of(
                            new FormAnswerRequest(requiredQuestion.getId(), "답변" + userId)
                    );

                    ReservationRequest request = new ReservationRequest(
                            schedule.getId(),
                            "후발주자" + userId,
                            "0108888" + String.format("%04d", userId),
                            1,
                            answers
                    );

                    reservationService.createReservation(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 정원 초과 예외
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 잔여 좌석 2명만큼만 성공
        assertThat(successCount.get()).isEqualTo(2);

        EventSchedule updatedSchedule = eventScheduleRepository.findById(schedule.getId()).get();
        assertThat(updatedSchedule.getReservedCount()).isEqualTo(10); // 8 + 2 = 10
        assertThat(updatedSchedule.getAvailableSeats()).isEqualTo(0);
    }
}
