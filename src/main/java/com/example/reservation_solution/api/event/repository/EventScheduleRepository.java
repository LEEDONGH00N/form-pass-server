package com.example.reservation_solution.api.event.repository;

import com.example.reservation_solution.api.event.domain.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, Long>, EventScheduleRepositoryCustom {
}
