package com.example.reservation_solution.repository;

import com.example.reservation_solution.domain.Event;
import com.example.reservation_solution.domain.Host;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByHost(Host host);

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.schedules " +
            "WHERE e.id = :eventId")
    Optional<Event> findByIdWithDetails(@Param("eventId") Long eventId);
}
