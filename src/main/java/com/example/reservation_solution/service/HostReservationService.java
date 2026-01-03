package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.*;
import com.example.reservation_solution.dto.*;
import com.example.reservation_solution.global.util.EncryptionUtils;
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
    private final EncryptionUtils encryptionUtils;

    public DashboardResponse getDashboardStats(Long eventId, String hostEmail) {
        Event event = validateHostOwnership(eventId, hostEmail);

        int totalSeats = event.getSchedules().stream()
                .mapToInt(EventSchedule::getMaxCapacity)
                .sum();

        int reservedCount = event.getSchedules().stream()
                .mapToInt(EventSchedule::getReservedCount)
                .sum();

        double reservationRate = totalSeats > 0 ? (reservedCount * 100.0 / totalSeats) : 0.0;

        int checkedInCount = event.getSchedules().stream()
                .flatMap(schedule -> reservationRepository.findByEventScheduleIdAndStatus(
                                schedule.getId(), ReservationStatus.CONFIRMED).stream())
                .filter(r -> r.getIsCheckedIn())
                .mapToInt(Reservation::getTicketCount)
                .sum();

        int availableSeats = totalSeats - reservedCount;

        return new DashboardResponse(totalSeats, reservedCount, reservationRate, checkedInCount, availableSeats);
    }

    public Page<ReservationListResponse> getReservationList(Long eventId, Long scheduleId, String searchKeyword, String hostEmail, Pageable pageable) {
        Event event = validateHostOwnership(eventId, hostEmail);

        Page<Reservation> reservations;
        boolean hasKeyword = searchKeyword != null && !searchKeyword.trim().isEmpty();

        if (scheduleId != null) {
            validateScheduleBelongsToEvent(scheduleId, eventId);
            reservations = hasKeyword
                    ? reservationRepository.findByEventScheduleIdAndKeyword(scheduleId, searchKeyword.trim(), pageable)
                    : reservationRepository.findByEventScheduleId(scheduleId, pageable);
        } else {
            List<Long> scheduleIds = event.getSchedules().stream()
                    .map(EventSchedule::getId)
                    .toList();

            reservations = hasKeyword
                    ? reservationRepository.findByEventScheduleIdInAndKeyword(scheduleIds, searchKeyword.trim(), pageable)
                    : reservationRepository.findByEventScheduleIdIn(scheduleIds, pageable);
        }

        return reservations.map(ReservationListResponse::from);
    }

    private void validateScheduleBelongsToEvent(Long scheduleId, Long eventId) {
        EventSchedule schedule = loadEventScheduleOrThrow(scheduleId);

        if (!schedule.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("해당 스케줄은 이 이벤트에 속하지 않습니다.");
        }
    }

    public ReservationResponse getReservationDetail(Long reservationId, String hostEmail) {
        Reservation reservation = loadReservationWithDetailsOrThrow(reservationId);
        validateHostOwnership(reservation, hostEmail);

        return ReservationResponse.from(reservation, encryptionUtils);
    }

    @Transactional
    public void cancelReservation(Long reservationId, String hostEmail) {
        Reservation reservation = loadReservationOrThrow(reservationId);
        validateHostOwnership(reservation, hostEmail);
        reservation.cancel();
        EventSchedule schedule = reservation.getEventSchedule();
        schedule.decrementReservedCount(reservation.getTicketCount());
    }

    @Transactional
    public CheckinResponse checkin(CheckinRequest request, String hostEmail) {
        Reservation reservation = loadReservationByQrTokenOrThrow(request.qrToken());
        return performCheckin(reservation, hostEmail);
    }

    @Transactional
    public CheckinResponse manualCheckin(Long reservationId, String hostEmail) {
        Reservation reservation = loadReservationOrThrow(reservationId);
        return performCheckin(reservation, hostEmail);
    }

    private CheckinResponse performCheckin(Reservation reservation, String hostEmail) {
        validateHostOwnership(reservation, hostEmail);
        reservation.checkIn();
        return new CheckinResponse(
                "입장 완료",
                reservation.getGuestName(),
                reservation.getTicketCount()
        );
    }

    public List<ScheduleStatusResponse> getScheduleStatus(Long eventId, String hostEmail) {
        Event event = validateHostOwnership(eventId, hostEmail);

        return event.getSchedules().stream()
                .map(schedule -> {
                    List<Reservation> reservations = reservationRepository
                            .findByEventScheduleIdAndStatus(schedule.getId(), ReservationStatus.CONFIRMED);

                    List<SimpleReservationDto> reservationDtos = reservations.stream()
                            .map(SimpleReservationDto::from)
                            .toList();

                    int currentCount = reservations.stream()
                            .mapToInt(Reservation::getTicketCount)
                            .sum();

                    return new ScheduleStatusResponse(
                            schedule.getId(),
                            schedule.getStartTime().toString(),
                            schedule.getEndTime().toString(),
                            schedule.getMaxCapacity(),
                            currentCount,
                            reservationDtos
                    );
                })
                .toList();
    }

    private Event validateHostOwnership(Long eventId, String hostEmail) {
        Event event = loadEventOrThrow(eventId);

        if (!event.getHost().getEmail().equals(hostEmail)) {
            throw new IllegalArgumentException("해당 이벤트에 대한 권한이 없습니다.");
        }

        return event;
    }

    private void validateHostOwnership(Reservation reservation, String hostEmail) {
        Event event = reservation.getEventSchedule().getEvent();
        if (!event.getHost().getEmail().equals(hostEmail)) {
            throw new IllegalArgumentException("해당 예약에 대한 권한이 없습니다.");
        }
    }

    private Event loadEventOrThrow(Long eventId) {
        return eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));
    }

    private EventSchedule loadEventScheduleOrThrow(Long scheduleId) {
        return eventScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));
    }

    private Reservation loadReservationOrThrow(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
    }

    private Reservation loadReservationWithDetailsOrThrow(Long reservationId) {
        return reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
    }

    private Reservation loadReservationByQrTokenOrThrow(String qrToken) {
        return reservationRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 QR 토큰입니다."));
    }
}
