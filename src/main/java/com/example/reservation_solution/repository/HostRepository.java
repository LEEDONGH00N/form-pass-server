package com.example.reservation_solution.repository;

import com.example.reservation_solution.domain.Host;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface HostRepository extends JpaRepository<Host, Long> {
    Optional<Host> findByEmail(String email);
    boolean existsByEmail(String email);
}
