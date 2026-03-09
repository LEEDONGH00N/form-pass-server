package com.example.reservation_solution.api.reservation.service;

import com.example.reservation_solution.api.reservation.dto.ReservationRequest;
import com.example.reservation_solution.api.reservation.dto.ReservationResponse;
import com.example.reservation_solution.global.exception.LockAcquisitionException;
import com.example.reservation_solution.global.lock.LockExecutor;
import com.example.reservation_solution.global.lock.LockKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationFacade {

    private final ReservationService reservationService;
    private final LockExecutor lockExecutor;

    public ReservationResponse createReservation(ReservationRequest request) {
        String lockKey = LockKeyGenerator.schedule(request.getScheduleId());
        try {
            return lockExecutor.executeWithLock(lockKey,
                    () -> reservationService.createReservation(request));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw LockAcquisitionException.interrupted();
        }
    }

    public void cancelReservation(Long reservationId) {
        Long scheduleId = reservationService.getScheduleIdByReservationId(reservationId);
        String lockKey = LockKeyGenerator.schedule(scheduleId);
        try {
            lockExecutor.executeWithLock(lockKey,
                    () -> reservationService.cancelReservation(reservationId));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw LockAcquisitionException.interrupted();
        }
    }
}
