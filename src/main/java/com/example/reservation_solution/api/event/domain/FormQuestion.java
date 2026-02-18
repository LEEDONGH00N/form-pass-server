package com.example.reservation_solution.api.event.domain;

import com.example.reservation_solution.global.common.domain.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "form_questions")
public class FormQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType;

    @Column(nullable = false)
    private Boolean isRequired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Builder
    public FormQuestion(String questionText, QuestionType questionType, Boolean isRequired) {
        this.questionText = questionText;
        this.questionType = questionType;
        this.isRequired = isRequired;
    }

    public static FormQuestion create(String questionText, QuestionType questionType, Boolean isRequired) {
        return FormQuestion.builder()
                .questionText(questionText)
                .questionType(questionType)
                .isRequired(isRequired)
                .build();
    }

    public void assignEvent(Event event) {
        this.event = event;
    }
}
