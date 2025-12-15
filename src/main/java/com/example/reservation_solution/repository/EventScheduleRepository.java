package com.example.reservation_solution.repository;

import com.example.reservation_solution.domain.EventSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from EventSchedule s where s.id = :id")
    Optional<EventSchedule> findByIdWithLock(@Param("id") Long id);
}
