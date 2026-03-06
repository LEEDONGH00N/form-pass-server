package com.example.reservation_solution.api.event.repository;

import com.example.reservation_solution.api.event.domain.Event;

import java.util.Optional;

public interface EventRepositoryCustom {

    Optional<Event> findByIdWithSchedules(Long eventId);

    Optional<Event> findByEventCodeWithSchedules(String eventCode);
}
