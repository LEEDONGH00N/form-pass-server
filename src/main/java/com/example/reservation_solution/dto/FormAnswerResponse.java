package com.example.reservation_solution.dto;

import com.example.reservation_solution.domain.FormAnswer;
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
