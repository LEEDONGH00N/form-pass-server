package com.example.reservation_solution.api.reservation.repository;

import com.example.reservation_solution.api.reservation.domain.FormAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FormAnswerRepository extends JpaRepository<FormAnswer, Long> {

    @Query("SELECT COUNT(fa) > 0 FROM FormAnswer fa WHERE fa.formQuestion.event.id = :eventId")
    boolean existsByEventId(@Param("eventId") Long eventId);
}
