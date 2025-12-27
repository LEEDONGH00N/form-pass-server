package com.example.reservation_solution.service;

import com.example.reservation_solution.domain.*;
import com.example.reservation_solution.dto.FormAnswerRequest;
import com.example.reservation_solution.dto.ReservationRequest;
import com.example.reservation_solution.dto.ReservationResponse;
import com.example.reservation_solution.dto.ReservationLookupResponse;
import com.example.reservation_solution.repository.EventScheduleRepository;
import com.example.reservation_solution.repository.FormAnswerRepository;
import com.example.reservation_solution.repository.FormQuestionRepository;
import com.example.reservation_solution.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

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
        EventSchedule schedule = eventScheduleRepository.findByIdWithLock(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        checkDuplicateReservation(request);

        int ticketCount = request.getTicketCount() != null && request.getTicketCount() > 0
                ? request.getTicketCount() : 1;

        validateCapacity(schedule, ticketCount);

        IntStream.range(0, ticketCount)
                .forEach(i -> schedule.incrementReservedCount());

        Reservation reservation = Reservation.create(
                schedule,
                request.getGuestName(),
                request.getGuestPhoneNumber(),
                ticketCount
        );

        processFormAnswers(request, reservation, schedule);
        validateRequiredQuestions(reservation, schedule);

        Reservation savedReservation = reservationRepository.save(reservation);
        return ReservationResponse.from(savedReservation);
    }

    private void checkDuplicateReservation(ReservationRequest request) {
        if (request.getGuestPhoneNumber() != null && !request.getGuestPhoneNumber().trim().isEmpty()) {
            boolean alreadyReserved = reservationRepository.existsByEventScheduleIdAndGuestPhoneNumberAndStatus(
                    request.getScheduleId(),
                    request.getGuestPhoneNumber(),
                    ReservationStatus.CONFIRMED
            );

            if (alreadyReserved) {
                throw new IllegalStateException("이미 해당 스케줄에 예약하셨습니다.");
            }
        }
    }

    private void validateCapacity(EventSchedule schedule, Integer ticketCount) {
        if (schedule.getReservedCount() + ticketCount > schedule.getMaxCapacity()) {
            if (schedule.getReservedCount() >= schedule.getMaxCapacity()) {
                throw new IllegalStateException("이미 매진된 스케줄입니다.");
            }
            throw new IllegalStateException("잔여 좌석이 부족합니다. (신청 인원: " + ticketCount +
                    ", 잔여 좌석: " + schedule.getAvailableSeats() + ")");
        }
    }

    private void processFormAnswers(ReservationRequest request, Reservation reservation, EventSchedule schedule) {
        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            request.getAnswers().stream()
                    .map(answerRequest -> {
                        FormQuestion question = formQuestionRepository.findById(answerRequest.questionId())
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 질문입니다."));

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
        formQuestionRepository.findByEventIdOrderById(schedule.getEvent().getId()).stream()
                .filter(question -> question.getIsRequired())
                .forEach(requiredQuestion -> {
                    boolean hasAnswer = reservation.getFormAnswers().stream()
                            .anyMatch(answer -> answer.getFormQuestion().getId().equals(requiredQuestion.getId()));

                    if (!hasAnswer) {
                        throw new IllegalArgumentException("필수 질문에 답변해야 합니다: " + requiredQuestion.getQuestionText());
                    }
                });
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

        if (reservation.getIsCheckedIn()) {
            throw new IllegalStateException("이미 입장 완료된 티켓은 취소할 수 없습니다.");
        }

        reservation.cancel();
        EventSchedule schedule = reservation.getEventSchedule();
        schedule.decrementReservedCount(reservation.getTicketCount());
    }

    public List<ReservationLookupResponse> lookupReservations(String guestName, String guestPhoneNumber) {
        List<Reservation> reservations = reservationRepository.findByGuestNameAndGuestPhoneNumberAndStatus(
                guestName,
                guestPhoneNumber,
                ReservationStatus.CONFIRMED
        );
        return reservations.stream()
                .map(ReservationLookupResponse::from)
                .toList();
    }
}
