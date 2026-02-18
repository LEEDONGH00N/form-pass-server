package com.example.reservation_solution.api.event.repository;

import com.example.reservation_solution.api.event.domain.FormQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormQuestionRepository extends JpaRepository<FormQuestion, Long> {

    List<FormQuestion> findByEventIdOrderById(Long eventId);
}
