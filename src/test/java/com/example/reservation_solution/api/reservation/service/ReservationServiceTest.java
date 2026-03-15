package com.example.reservation_solution.api.reservation.service;

import com.example.reservation_solution.api.auth.domain.Host;
import com.example.reservation_solution.api.event.domain.EventSchedule;
import com.example.reservation_solution.api.event.domain.FormQuestion;
import com.example.reservation_solution.api.event.domain.Event;
import com.example.reservation_solution.api.event.repository.EventScheduleRepository;
import com.example.reservation_solution.api.event.repository.FormQuestionRepository;
import com.example.reservation_solution.api.reservation.domain.Reservation;
import com.example.reservation_solution.api.reservation.domain.ReservationStatus;
import com.example.reservation_solution.api.reservation.dto.ReservationLookupResponse;
import com.example.reservation_solution.api.reservation.dto.ReservationRequest;
import com.example.reservation_solution.api.reservation.dto.ReservationResponse;
import com.example.reservation_solution.api.reservation.repository.ReservationRepository;
import com.example.reservation_solution.global.util.EncryptionUtils;
import com.example.reservation_solution.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EventScheduleRepository eventScheduleRepository;

    @Mock
    private FormQuestionRepository formQuestionRepository;

    @Mock
    private EncryptionUtils encryptionUtils;

    @Test
    @DisplayName("예약 생성 성공")
    void createReservation_success() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        ReservationRequest request = TestFixtures.createReservationRequest(1L);

        given(eventScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
        given(encryptionUtils.encrypt("01012345678")).willReturn("encryptedPhone");
        given(reservationRepository.existsByEventScheduleIdAndGuestPhoneNumberAndStatus(
                eq(1L), eq("encryptedPhone"), eq(ReservationStatus.CONFIRMED))).willReturn(false);
        given(formQuestionRepository.findByEventIdOrderById(anyLong())).willReturn(Collections.emptyList());
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        given(encryptionUtils.decrypt("encryptedPhone")).willReturn("01012345678");

        // when
        ReservationResponse response = reservationService.createReservation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getGuestName()).isEqualTo("홍길동");
        assertThat(response.getGuestPhoneNumber()).isEqualTo("01012345678");
        assertThat(schedule.getReservedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 스케줄로 예약 시 예외가 발생한다")
    void createReservation_shouldThrowException_whenScheduleNotFound() {
        // given
        ReservationRequest request = TestFixtures.createReservationRequest(999L);
        given(eventScheduleRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 스케줄입니다.");
    }

    @Test
    @DisplayName("동일 스케줄에 동일 전화번호로 중복 예약 시 예외가 발생한다")
    void createReservation_shouldThrowException_whenDuplicateReservation() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        ReservationRequest request = TestFixtures.createReservationRequest(1L);

        given(eventScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
        given(encryptionUtils.encrypt("01012345678")).willReturn("encryptedPhone");
        given(reservationRepository.existsByEventScheduleIdAndGuestPhoneNumberAndStatus(
                eq(1L), eq("encryptedPhone"), eq(ReservationStatus.CONFIRMED))).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 해당 스케줄에 예약하셨습니다.");
    }

    @Test
    @DisplayName("정원 초과 시 예외가 발생한다")
    void createReservation_shouldThrowException_whenCapacityExceeded() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 1);
        ReflectionTestUtils.setField(schedule, "reservedCount", 1);
        ReservationRequest request = TestFixtures.createReservationRequest(1L);

        given(eventScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
        given(encryptionUtils.encrypt("01012345678")).willReturn("encryptedPhone");
        given(reservationRepository.existsByEventScheduleIdAndGuestPhoneNumberAndStatus(
                eq(1L), eq("encryptedPhone"), eq(ReservationStatus.CONFIRMED))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("예약 가능 좌석을 초과했습니다");
    }

    @Test
    @DisplayName("필수 질문에 답변하지 않으면 예외가 발생한다")
    void createReservation_shouldThrowException_whenRequiredQuestionNotAnswered() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        Event event = schedule.getEvent();
        FormQuestion requiredQuestion = TestFixtures.createFormQuestion(event, true);

        ReservationRequest request = TestFixtures.createReservationRequest(1L);

        given(eventScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
        given(encryptionUtils.encrypt("01012345678")).willReturn("encryptedPhone");
        given(reservationRepository.existsByEventScheduleIdAndGuestPhoneNumberAndStatus(
                eq(1L), eq("encryptedPhone"), eq(ReservationStatus.CONFIRMED))).willReturn(false);
        given(formQuestionRepository.findByEventIdOrderById(event.getId()))
                .willReturn(List.of(requiredQuestion));

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("필수 질문에 답변해야 합니다");
    }

    @Test
    @DisplayName("전화번호가 암호화되어 저장된다")
    void createReservation_shouldEncryptPhoneNumber() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        ReservationRequest request = TestFixtures.createReservationRequest(1L);

        given(eventScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
        given(encryptionUtils.encrypt("01012345678")).willReturn("encryptedPhone");
        given(reservationRepository.existsByEventScheduleIdAndGuestPhoneNumberAndStatus(
                eq(1L), eq("encryptedPhone"), eq(ReservationStatus.CONFIRMED))).willReturn(false);
        given(formQuestionRepository.findByEventIdOrderById(anyLong())).willReturn(Collections.emptyList());
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        given(encryptionUtils.decrypt("encryptedPhone")).willReturn("01012345678");

        // when
        reservationService.createReservation(request);

        // then
        verify(encryptionUtils).encrypt("01012345678");
    }

    @Test
    @DisplayName("예약 ID로 조회 성공")
    void getReservation_success() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        Reservation reservation = TestFixtures.createReservation(schedule);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));
        given(encryptionUtils.decrypt("encryptedPhone")).willReturn("01012345678");

        // when
        ReservationResponse response = reservationService.getReservation(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 예약 ID로 조회 시 예외가 발생한다")
    void getReservation_shouldThrowException_whenNotFound() {
        // given
        given(reservationRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.getReservation(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 예약입니다.");
    }

    @Test
    @DisplayName("QR 토큰으로 예약 조회 성공")
    void getReservationByQrToken_success() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        Reservation reservation = TestFixtures.createReservation(schedule);

        given(reservationRepository.findByQrToken("test-token")).willReturn(Optional.of(reservation));
        given(encryptionUtils.decrypt("encryptedPhone")).willReturn("01012345678");

        // when
        ReservationResponse response = reservationService.getReservationByQrToken("test-token");

        // then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 QR 토큰으로 조회 시 예외가 발생한다")
    void getReservationByQrToken_shouldThrowException_whenNotFound() {
        // given
        given(reservationRepository.findByQrToken("invalid-token")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.getReservationByQrToken("invalid-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 예약입니다.");
    }

    @Test
    @DisplayName("예약 취소 성공")
    void cancelReservation_success() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        ReflectionTestUtils.setField(schedule, "reservedCount", 2);
        Reservation reservation = TestFixtures.createReservation(schedule, 2);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        // when
        reservationService.cancelReservation(1L);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(schedule.getReservedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("이미 취소된 예약 취소 시 예외가 발생한다")
    void cancelReservation_shouldThrowException_whenAlreadyCancelled() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        ReflectionTestUtils.setField(schedule, "reservedCount", 1);
        Reservation reservation = TestFixtures.createReservation(schedule);
        reservation.cancel();
        schedule.decrementReservedCount(1);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 취소된 예약입니다.");
    }

    @Test
    @DisplayName("이미 체크인된 예약 취소 시 예외가 발생한다")
    void cancelReservation_shouldThrowException_whenAlreadyCheckedIn() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        ReflectionTestUtils.setField(schedule, "reservedCount", 1);
        Reservation reservation = TestFixtures.createReservation(schedule);
        reservation.checkIn();

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 입장 완료된 티켓은 취소할 수 없습니다.");
    }

    @Test
    @DisplayName("게스트 이름과 전화번호로 예약 조회 성공")
    void lookupReservations_success() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        Reservation reservation = TestFixtures.createReservation(schedule);

        given(encryptionUtils.encrypt("01012345678")).willReturn("encryptedPhone");
        given(reservationRepository.findByGuestInfoAndStatus("홍길동", "encryptedPhone", ReservationStatus.CONFIRMED))
                .willReturn(List.of(reservation));

        // when
        List<ReservationLookupResponse> responses = reservationService.lookupReservations("홍길동", "01012345678");

        // then
        assertThat(responses).hasSize(1);
    }
}
