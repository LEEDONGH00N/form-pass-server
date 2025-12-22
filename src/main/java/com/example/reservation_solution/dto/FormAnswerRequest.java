package com.example.reservation_solution.dto;

public record FormAnswerRequest(
    Long questionId,
    String answerText
) {
}
