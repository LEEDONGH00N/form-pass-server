package com.example.reservation_solution.repository;

import com.example.reservation_solution.domain.Reservation;
import com.example.reservation_solution.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByQrToken(String qrToken);

    @Query("select r from Reservation r where r.eventSchedule.id = :scheduleId and r.guestPhoneNumber = :phoneNumber and r.status = :status")
    Optional<Reservation> findByEventScheduleIdAndPhoneNumberAndStatus(
            @Param("scheduleId") Long scheduleId,
            @Param("phoneNumber") String phoneNumber,
            @Param("status") ReservationStatus status
    );

    boolean existsByEventScheduleIdAndGuestPhoneNumberAndStatus(Long scheduleId, String guestPhoneNumber, ReservationStatus status);

    List<Reservation> findByEventScheduleIdAndStatus(Long scheduleId, ReservationStatus status);

    Page<Reservation> findByEventScheduleId(Long scheduleId, Pageable pageable);

    Page<Reservation> findByEventScheduleIdIn(List<Long> scheduleIds, Pageable pageable);

    @EntityGraph(attributePaths = {"eventSchedule", "formAnswers", "formAnswers.formQuestion"})
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdWithDetails(@Param("id") Long id);
}
