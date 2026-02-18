package com.example.reservation_solution.api.reservation.repository;

import com.example.reservation_solution.api.reservation.domain.Reservation;
import com.example.reservation_solution.api.reservation.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @EntityGraph(attributePaths = {"eventSchedule", "eventSchedule.event", "formAnswers", "formAnswers.formQuestion"})
    @Query("select r from Reservation r where r.guestName = :guestName and r.guestPhoneNumber = :guestPhoneNumber and r.status = :status order by r.createdAt desc")
    List<Reservation> findByGuestNameAndGuestPhoneNumberAndStatus(
            @Param("guestName") String guestName,
            @Param("guestPhoneNumber") String guestPhoneNumber,
            @Param("status") ReservationStatus status
    );

    @Query("select r from Reservation r where r.eventSchedule.id = :scheduleId " +
            "and (r.guestName like %:keyword% or r.guestPhoneNumber like %:keyword%)")
    Page<Reservation> findByEventScheduleIdAndKeyword(
            @Param("scheduleId") Long scheduleId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("select r from Reservation r where r.eventSchedule.id in :scheduleIds " +
            "and (r.guestName like %:keyword% or r.guestPhoneNumber like %:keyword%)")
    Page<Reservation> findByEventScheduleIdInAndKeyword(
            @Param("scheduleIds") List<Long> scheduleIds,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
