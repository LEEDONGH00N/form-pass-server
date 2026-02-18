package com.example.reservation_solution.api.event.dto;

import com.example.reservation_solution.api.event.domain.FormQuestion;
import com.example.reservation_solution.api.event.domain.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String questionText;
    private QuestionType questionType;
    private Boolean isRequired;

    public static QuestionResponse from(FormQuestion question) {
        return new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getIsRequired()
        );
    }
}
