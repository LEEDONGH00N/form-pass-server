package com.example.reservation_solution.api.host.service;

import com.example.reservation_solution.api.auth.domain.Host;
import com.example.reservation_solution.api.event.domain.Event;
import com.example.reservation_solution.api.event.domain.EventSchedule;
import com.example.reservation_solution.api.event.repository.EventRepository;
import com.example.reservation_solution.api.event.repository.EventScheduleRepository;
import com.example.reservation_solution.api.host.dto.CheckinRequest;
import com.example.reservation_solution.api.host.dto.CheckinResponse;
import com.example.reservation_solution.api.host.dto.DashboardResponse;
import com.example.reservation_solution.api.host.dto.ScheduleStatusResponse;
import com.example.reservation_solution.api.reservation.domain.Reservation;
import com.example.reservation_solution.api.reservation.domain.ReservationStatus;
import com.example.reservation_solution.api.reservation.dto.ReservationResponse;
import com.example.reservation_solution.api.reservation.repository.ReservationRepository;
import com.example.reservation_solution.api.auth.repository.HostRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HostReservationServiceTest {

    @InjectMocks
    private HostReservationService hostReservationService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EventScheduleRepository eventScheduleRepository;

    @Mock
    private HostRepository hostRepository;

    @Mock
    private EncryptionUtils encryptionUtils;

    @Test
    @DisplayName("대시보드 통계 정상 조회")
    void getDashboardStats_success() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);
        EventSchedule schedule = TestFixtures.createEventSchedule(100);
        ReflectionTestUtils.setField(schedule, "reservedCount", 10);
        event.addSchedule(schedule);

        given(eventRepository.findByIdWithSchedules(1L)).willReturn(Optional.of(event));
        given(reservationRepository.findByEventScheduleIdAndStatus(eq(1L), eq(ReservationStatus.CONFIRMED)))
                .willReturn(Collections.emptyList());

        // when
        DashboardResponse response = hostReservationService.getDashboardStats(1L, "host@example.com");

        // then
        assertThat(response.totalSeats()).isEqualTo(100);
        assertThat(response.reservedCount()).isEqualTo(10);
        assertThat(response.availableSeats()).isEqualTo(90);
    }

    @Test
    @DisplayName("대시보드 조회 시 이벤트가 존재하지 않으면 예외가 발생한다")
    void getDashboardStats_shouldThrowException_whenEventNotFound() {
        // given
        given(eventRepository.findByIdWithSchedules(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hostReservationService.getDashboardStats(999L, "host@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 이벤트입니다.");
    }

    @Test
    @DisplayName("대시보드 조회 시 권한이 없으면 예외가 발생한다")
    void getDashboardStats_shouldThrowException_whenUnauthorized() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);

        given(eventRepository.findByIdWithSchedules(1L)).willReturn(Optional.of(event));

        // when & then
        assertThatThrownBy(() -> hostReservationService.getDashboardStats(1L, "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 이벤트에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("호스트 예약 취소 성공")
    void cancelReservation_success() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        ReflectionTestUtils.setField(schedule, "reservedCount", 1);
        Reservation reservation = TestFixtures.createReservation(schedule);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        // when
        hostReservationService.cancelReservation(1L, "host@example.com");

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(schedule.getReservedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("호스트 예약 취소 시 권한이 없으면 예외가 발생한다")
    void cancelReservation_shouldThrowException_whenUnauthorized() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        ReflectionTestUtils.setField(schedule, "reservedCount", 1);
        Reservation reservation = TestFixtures.createReservation(schedule);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> hostReservationService.cancelReservation(1L, "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 예약에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("QR 체크인 성공")
    void checkin_success() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        Reservation reservation = TestFixtures.createReservation(schedule);
        CheckinRequest request = new CheckinRequest(reservation.getQrToken());

        given(reservationRepository.findByQrToken(reservation.getQrToken())).willReturn(Optional.of(reservation));

        // when
        CheckinResponse response = hostReservationService.checkin(request, "host@example.com");

        // then
        assertThat(response.message()).isEqualTo("입장 완료");
        assertThat(reservation.getIsCheckedIn()).isTrue();
    }

    @Test
    @DisplayName("이미 체크인된 예약의 QR 체크인 시 예외가 발생한다")
    void checkin_shouldThrowException_whenAlreadyCheckedIn() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        Reservation reservation = TestFixtures.createReservation(schedule);
        reservation.checkIn();
        CheckinRequest request = new CheckinRequest(reservation.getQrToken());

        given(reservationRepository.findByQrToken(reservation.getQrToken())).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> hostReservationService.checkin(request, "host@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 체크인이 완료된 예약입니다.");
    }

    @Test
    @DisplayName("취소된 예약의 QR 체크인 시 예외가 발생한다")
    void checkin_shouldThrowException_whenCancelled() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        ReflectionTestUtils.setField(schedule, "reservedCount", 1);
        Reservation reservation = TestFixtures.createReservation(schedule);
        reservation.cancel();
        CheckinRequest request = new CheckinRequest(reservation.getQrToken());

        given(reservationRepository.findByQrToken(reservation.getQrToken())).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> hostReservationService.checkin(request, "host@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("취소된 예약은 체크인할 수 없습니다.");
    }

    @Test
    @DisplayName("수동 체크인 성공")
    void manualCheckin_success() {
        // given
        Host host = TestFixtures.createHost();
        EventSchedule schedule = TestFixtures.createEventScheduleWithEvent(host, 100);
        Reservation reservation = TestFixtures.createReservation(schedule);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        // when
        CheckinResponse response = hostReservationService.manualCheckin(1L, "host@example.com");

        // then
        assertThat(response.message()).isEqualTo("입장 완료");
        assertThat(reservation.getIsCheckedIn()).isTrue();
    }

    @Test
    @DisplayName("스케줄 상태 조회 성공")
    void getScheduleStatus_success() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);
        EventSchedule schedule = TestFixtures.createEventSchedule(50);
        event.addSchedule(schedule);

        given(eventRepository.findByIdWithSchedules(1L)).willReturn(Optional.of(event));
        given(reservationRepository.findByEventScheduleIdAndStatus(eq(1L), eq(ReservationStatus.CONFIRMED)))
                .willReturn(Collections.emptyList());

        // when
        List<ScheduleStatusResponse> responses = hostReservationService.getScheduleStatus(1L, "host@example.com");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getMaxCapacity()).isEqualTo(50);
    }
}
