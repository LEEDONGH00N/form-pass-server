package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.*;
import com.example.reservation_solution.dto.FormAnswerRequest;
import com.example.reservation_solution.dto.ReservationRequest;
import com.example.reservation_solution.dto.ReservationResponse;
import com.example.reservation_solution.repository.EventScheduleRepository;
import com.example.reservation_solution.repository.FormAnswerRepository;
import com.example.reservation_solution.repository.FormQuestionRepository;
import com.example.reservation_solution.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final FormQuestionRepository formQuestionRepository;
    private final FormAnswerRepository formAnswerRepository;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        // 1. 비관적 락을 사용하여 EventSchedule 조회 (동시성 제어의 핵심!)
        EventSchedule schedule = eventScheduleRepository.findByIdWithLock(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        // 2. 중복 예약 체크 (같은 스케줄에 같은 전화번호로 이미 예약이 있는지)
        boolean alreadyReserved = reservationRepository.existsByEventScheduleIdAndGuestPhoneNumberAndStatus(
                request.getScheduleId(),
                request.getGuestPhoneNumber(),
                ReservationStatus.CONFIRMED
        );

        if (alreadyReserved) {
            throw new IllegalStateException("이미 해당 스케줄에 예약하셨습니다.");
        }

        // 3. ticketCount 검증
        Integer ticketCount = request.getTicketCount() != null && request.getTicketCount() > 0
                ? request.getTicketCount() : 1;

        // 4. 잔여 좌석 확인 (락이 걸린 상태에서 실행됨)
        if (schedule.getReservedCount() + ticketCount > schedule.getMaxCapacity()) {
            throw new IllegalStateException("잔여 좌석이 부족합니다. (신청 인원: " + ticketCount +
                    ", 잔여 좌석: " + schedule.getAvailableSeats() + ")");
        }

        // 5. 예약 카운트 증가
        for (int i = 0; i < ticketCount; i++) {
            schedule.incrementReservedCount();
        }

        // 6. 예약 생성
        Reservation reservation = Reservation.create(
                schedule,
                request.getGuestName(),
                request.getGuestPhoneNumber(),
                ticketCount
        );

        // 7. 질문 답변 처리
        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            for (FormAnswerRequest answerRequest : request.getAnswers()) {
                FormQuestion question = formQuestionRepository.findById(answerRequest.getQuestionId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 질문입니다."));

                // 필수 질문 검증
                if (question.getIsRequired() &&
                    (answerRequest.getAnswerText() == null || answerRequest.getAnswerText().trim().isEmpty())) {
                    throw new IllegalArgumentException("필수 질문에 답변해야 합니다: " + question.getQuestionText());
                }

                FormAnswer formAnswer = FormAnswer.create(question, answerRequest.getAnswerText());
                reservation.addFormAnswer(formAnswer);
            }
        }

        // 8. 필수 질문에 대한 답변이 모두 있는지 확인
        List<FormQuestion> requiredQuestions = formQuestionRepository.findByEventIdOrderById(
                schedule.getEvent().getId()
        ).stream()
                .filter(FormQuestion::getIsRequired)
                .toList();

        for (FormQuestion requiredQuestion : requiredQuestions) {
            boolean hasAnswer = reservation.getFormAnswers().stream()
                    .anyMatch(answer -> answer.getFormQuestion().getId().equals(requiredQuestion.getId()));

            if (!hasAnswer) {
                throw new IllegalArgumentException("필수 질문에 답변해야 합니다: " + requiredQuestion.getQuestionText());
            }
        }

        // 9. 예약 저장
        Reservation savedReservation = reservationRepository.save(reservation);

        return ReservationResponse.from(savedReservation);
    }

    public ReservationResponse getReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
        return ReservationResponse.from(reservation);
    }

    public ReservationResponse getReservationByQrToken(String qrToken) {
        Reservation reservation = reservationRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }

        reservation.cancel();
    }
}
