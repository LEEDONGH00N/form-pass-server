package com.example.reservation_solution.api.reservation.dto;

import com.example.reservation_solution.api.reservation.domain.FormAnswer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FormAnswerResponse {
    private Long questionId;
    private String questionText;
    private String answerText;

    public static FormAnswerResponse from(FormAnswer formAnswer) {
        return new FormAnswerResponse(
                formAnswer.getFormQuestion().getId(),
                formAnswer.getFormQuestion().getQuestionText(),
                formAnswer.getAnswerText()
        );
    }
}
