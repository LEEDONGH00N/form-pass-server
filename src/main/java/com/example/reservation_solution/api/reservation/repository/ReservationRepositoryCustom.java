package com.example.reservation_solution.api.reservation.repository;

import com.example.reservation_solution.api.reservation.domain.Reservation;
import com.example.reservation_solution.api.reservation.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ReservationRepositoryCustom {

    Optional<Reservation> findByScheduleIdAndPhoneNumberAndStatus(Long scheduleId, String phoneNumber, ReservationStatus status);

    Optional<Reservation> findByIdWithDetails(Long id);

    List<Reservation> findByGuestInfoAndStatus(String guestName, String guestPhoneNumber, ReservationStatus status);

    Page<Reservation> searchReservations(Long scheduleId, List<Long> scheduleIds, String keyword, Pageable pageable);
}
