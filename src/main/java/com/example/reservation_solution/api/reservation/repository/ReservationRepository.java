package com.example.reservation_solution.api.reservation.repository;

import com.example.reservation_solution.api.reservation.domain.Reservation;
import com.example.reservation_solution.api.reservation.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {

    Optional<Reservation> findByQrToken(String qrToken);

    boolean existsByEventScheduleIdAndGuestPhoneNumberAndStatus(Long scheduleId, String guestPhoneNumber, ReservationStatus status);

    List<Reservation> findByEventScheduleIdAndStatus(Long scheduleId, ReservationStatus status);

    Page<Reservation> findByEventScheduleId(Long scheduleId, Pageable pageable);

    Page<Reservation> findByEventScheduleIdIn(List<Long> scheduleIds, Pageable pageable);
}
