package com.example.reservation_solution.api.reservation.domain;

import com.example.reservation_solution.api.event.domain.FormQuestion;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "form_answers")
public class FormAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_question_id", nullable = false)
    private FormQuestion formQuestion;

    @Column(nullable = false)
    private String answerText;

    @Builder
    public FormAnswer(FormQuestion formQuestion, String answerText) {
        this.formQuestion = formQuestion;
        this.answerText = answerText;
    }

    public void assignReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public static FormAnswer create(FormQuestion formQuestion, String answerText) {
        return FormAnswer.builder()
                .formQuestion(formQuestion)
                .answerText(answerText)
                .build();
    }
}
