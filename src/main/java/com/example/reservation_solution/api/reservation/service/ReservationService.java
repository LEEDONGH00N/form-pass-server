package com.example.reservation_solution.api.reservation.service;

import com.example.reservation_solution.api.event.domain.*;
import com.example.reservation_solution.api.reservation.domain.*;
import com.example.reservation_solution.api.auth.domain.*;
import com.example.reservation_solution.api.reservation.dto.ReservationRequest;
import com.example.reservation_solution.api.reservation.dto.ReservationResponse;
import com.example.reservation_solution.api.reservation.dto.ReservationLookupResponse;
import com.example.reservation_solution.global.util.EncryptionUtils;
import com.example.reservation_solution.api.event.repository.EventScheduleRepository;
import com.example.reservation_solution.api.event.repository.FormQuestionRepository;
import com.example.reservation_solution.api.reservation.repository.ReservationRepository;
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
    private final EncryptionUtils encryptionUtils;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        EventSchedule schedule = loadEventScheduleWithLockOrThrow(request.getScheduleId());
        String encryptedPhoneNumber = encryptionUtils.encrypt(request.getGuestPhoneNumber());
        checkDuplicateReservation(request.getScheduleId(), encryptedPhoneNumber);
        schedule.incrementReservedCount(request.getTicketCount());
        Reservation reservation = Reservation.create(
                schedule,
                request.getGuestName(),
                encryptedPhoneNumber,
                request.getTicketCount()
        );
        processFormAnswers(request, reservation);
        validateRequiredQuestions(reservation, schedule);
        Reservation savedReservation = reservationRepository.save(reservation);
        return ReservationResponse.from(savedReservation, encryptionUtils);
    }

    private void checkDuplicateReservation(Long scheduleId, String encryptedPhoneNumber) {
        if (encryptedPhoneNumber != null && !encryptedPhoneNumber.trim().isEmpty()) {
            boolean alreadyReserved = reservationRepository.existsByEventScheduleIdAndGuestPhoneNumberAndStatus(
                    scheduleId,
                    encryptedPhoneNumber,
                    ReservationStatus.CONFIRMED
            );
            if (alreadyReserved) {
                throw new IllegalStateException("이미 해당 스케줄에 예약하셨습니다.");
            }
        }
    }

    private void processFormAnswers(ReservationRequest request, Reservation reservation) {
        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            request.getAnswers().stream()
                    .map(answerRequest -> {
                        FormQuestion question = loadFormQuestionOrThrow(answerRequest.questionId());
                        if (question.getIsRequired() &&
                            (answerRequest.answerText() == null || answerRequest.answerText().trim().isEmpty())) {
                            throw new IllegalArgumentException("필수 질문에 답변해야 합니다: " + question.getQuestionText());
                        }
                        return FormAnswer.create(question, answerRequest.answerText());
                    })
                    .forEach(reservation::addFormAnswer);
        }
    }

    private void validateRequiredQuestions(Reservation reservation, EventSchedule schedule) {
        formQuestionRepository.findByEventIdOrderById(schedule.getEvent().getId())
                .stream()
                .filter(FormQuestion::getIsRequired)
                .filter(question -> !isBuiltInQuestion(question.getQuestionText()))
                .forEach(requiredQuestion -> {
                    boolean hasAnswer = reservation.getFormAnswers().stream()
                            .anyMatch(answer -> answer.getFormQuestion().getId().equals(requiredQuestion.getId()));
                    if (!hasAnswer) {
                        throw new IllegalArgumentException("필수 질문에 답변해야 합니다: " + requiredQuestion.getQuestionText());
                    }
                });
    }

    private boolean isBuiltInQuestion(String questionText) {
        String normalized = questionText.toLowerCase().replaceAll("\\s+", "");
        return normalized.contains("이름") || normalized.contains("name") ||
               normalized.contains("전화") || normalized.contains("연락처") ||
               normalized.contains("핸드폰") || normalized.contains("phone");
    }

    public ReservationResponse getReservation(Long id) {
        Reservation reservation = loadReservationOrThrow(id);
        return ReservationResponse.from(reservation, encryptionUtils);
    }

    public ReservationResponse getReservationByQrToken(String qrToken) {
        Reservation reservation = loadReservationByQrTokenOrThrow(qrToken);
        return ReservationResponse.from(reservation, encryptionUtils);
    }

    @Transactional
    public void cancelReservation(Long id) {
        Reservation reservation = loadReservationOrThrow(id);
        reservation.cancel();
        EventSchedule schedule = reservation.getEventSchedule();
        schedule.decrementReservedCount(reservation.getTicketCount());
    }

    public List<ReservationLookupResponse> lookupReservations(String guestName, String guestPhoneNumber) {
        String encryptedPhoneNumber = encryptionUtils.encrypt(guestPhoneNumber);
        List<Reservation> reservations = reservationRepository.findByGuestNameAndGuestPhoneNumberAndStatus(
                guestName,
                encryptedPhoneNumber,
                ReservationStatus.CONFIRMED
        );
        return reservations.stream()
                .map(ReservationLookupResponse::from)
                .toList();
    }

    private EventSchedule loadEventScheduleWithLockOrThrow(Long scheduleId) {
        return eventScheduleRepository.findByIdWithLock(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));
    }

    private Reservation loadReservationOrThrow(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
    }

    private Reservation loadReservationByQrTokenOrThrow(String qrToken) {
        return reservationRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
    }

    private FormQuestion loadFormQuestionOrThrow(Long questionId) {
        return formQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 질문입니다."));
    }
}
