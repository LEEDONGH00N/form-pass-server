package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.*;
import com.example.reservation_solution.dto.*;
import com.example.reservation_solution.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostReservationService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final HostRepository hostRepository;

    public DashboardResponse getDashboardStats(Long eventId, String hostEmail) {
        Event event = validateHostOwnership(eventId, hostEmail);

        // 총 좌석 수
        Integer totalSeats = event.getSchedules().stream()
                .mapToInt(EventSchedule::getMaxCapacity)
                .sum();

        // 총 예약 인원 (ticketCount의 합)
        Integer reservedCount = event.getSchedules().stream()
                .mapToInt(EventSchedule::getReservedCount)
                .sum();

        // 예약률
        Double reservationRate = totalSeats > 0 ? (reservedCount * 100.0 / totalSeats) : 0.0;

        // 입장 완료 인원 (isCheckedIn이 true인 예약의 ticketCount 합)
        Integer checkedInCount = event.getSchedules().stream()
                .flatMap(schedule -> reservationRepository.findByEventScheduleIdAndStatus(
                                schedule.getId(), ReservationStatus.CONFIRMED).stream())
                .filter(Reservation::getIsCheckedIn)
                .mapToInt(Reservation::getTicketCount)
                .sum();

        // 잔여 좌석
        Integer availableSeats = totalSeats - reservedCount;

        return new DashboardResponse(totalSeats, reservedCount, reservationRate, checkedInCount, availableSeats);
    }

    public Page<ReservationListResponse> getReservationList(Long eventId, Long scheduleId, String hostEmail, Pageable pageable) {
        Event event = validateHostOwnership(eventId, hostEmail);

        Page<Reservation> reservations;
        if (scheduleId != null) {
            // 특정 스케줄 필터링
            EventSchedule schedule = eventScheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

            if (!schedule.getEvent().getId().equals(eventId)) {
                throw new IllegalArgumentException("해당 스케줄은 이 이벤트에 속하지 않습니다.");
            }

            reservations = reservationRepository.findByEventScheduleId(scheduleId, pageable);
        } else {
            // 전체 예약 조회
            List<Long> scheduleIds = event.getSchedules().stream()
                    .map(EventSchedule::getId)
                    .toList();

            reservations = reservationRepository.findByEventScheduleIdIn(scheduleIds, pageable);
        }

        return reservations.map(ReservationListResponse::from);
    }

    public ReservationResponse getReservationDetail(Long reservationId, String hostEmail) {
        Reservation reservation = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        // 호스트 권한 확인
        Event event = reservation.getEventSchedule().getEvent();
        if (!event.getHost().getEmail().equals(hostEmail)) {
            throw new IllegalArgumentException("해당 예약에 대한 권한이 없습니다.");
        }

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public void cancelReservation(Long reservationId, String hostEmail) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        // 호스트 권한 확인
        Event event = reservation.getEventSchedule().getEvent();
        if (!event.getHost().getEmail().equals(hostEmail)) {
            throw new IllegalArgumentException("해당 예약에 대한 권한이 없습니다.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }

        // 예약 취소
        reservation.cancel();

        // 재고 복구 (스케줄의 reservedCount 차감)
        EventSchedule schedule = reservation.getEventSchedule();
        schedule.decrementReservedCount(reservation.getTicketCount());
    }

    @Transactional
    public CheckinResponse checkin(CheckinRequest request, String hostEmail) {
        Reservation reservation = reservationRepository.findByQrToken(request.getQrToken())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 QR 토큰입니다."));

        // 호스트 권한 확인
        Event event = reservation.getEventSchedule().getEvent();
        if (!event.getHost().getEmail().equals(hostEmail)) {
            throw new IllegalArgumentException("해당 예약에 대한 권한이 없습니다.");
        }

        // 이미 입장했는지 확인
        if (reservation.getIsCheckedIn()) {
            throw new IllegalStateException("이미 입장 완료된 티켓입니다.");
        }

        // 취소된 예약인지 확인
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("취소된 예약입니다.");
        }

        // 체크인 처리
        reservation.checkIn();

        return new CheckinResponse(
                "입장 완료",
                reservation.getGuestName(),
                reservation.getTicketCount()
        );
    }

    private Event validateHostOwnership(Long eventId, String hostEmail) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        if (!event.getHost().getEmail().equals(hostEmail)) {
            throw new IllegalArgumentException("해당 이벤트에 대한 권한이 없습니다.");
        }

        return event;
    }
}
