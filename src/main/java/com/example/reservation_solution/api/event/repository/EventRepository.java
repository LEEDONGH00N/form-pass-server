package com.example.reservation_solution.api.event.repository;

import com.example.reservation_solution.api.event.domain.Event;
import com.example.reservation_solution.api.auth.domain.Host;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {

    List<Event> findByHost(Host host);

    boolean existsByEventCode(String eventCode);
}
