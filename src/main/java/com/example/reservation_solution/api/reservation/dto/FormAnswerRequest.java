package com.example.reservation_solution.api.reservation.dto;

public record FormAnswerRequest(
    Long questionId,
    String answerText
) {
}
