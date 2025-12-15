package com.example.reservation_solution.dto;

import com.example.reservation_solution.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {

    @NotBlank(message = "질문 내용은 필수입니다")
    private String questionText;

    @NotNull(message = "질문 타입은 필수입니다")
    private QuestionType questionType;

    @NotNull(message = "필수 여부는 필수입니다")
    private Boolean isRequired;
}
