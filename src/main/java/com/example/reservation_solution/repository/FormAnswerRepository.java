package com.example.reservation_solution.repository;

import com.example.reservation_solution.domain.FormAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormAnswerRepository extends JpaRepository<FormAnswer, Long> {
}
