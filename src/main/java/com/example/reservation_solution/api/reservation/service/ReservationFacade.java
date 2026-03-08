package com.example.reservation_solution.api.reservation.service;

import com.example.reservation_solution.api.reservation.dto.ReservationRequest;
import com.example.reservation_solution.api.reservation.dto.ReservationResponse;
import com.example.reservation_solution.global.lock.DistributedLockService;
import com.example.reservation_solution.global.lock.LockKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationFacade {

    private final ReservationService reservationService;
    private final DistributedLockService distributedLockService;

    public ReservationResponse createReservation(ReservationRequest request) {
        String lockKey = LockKeyGenerator.schedule(request.getScheduleId());
        return distributedLockService.executeWithLock(lockKey,
                () -> reservationService.createReservation(request));
    }

    public void cancelReservation(Long reservationId) {
        Long scheduleId = reservationService.getScheduleIdByReservationId(reservationId);
        String lockKey = LockKeyGenerator.schedule(scheduleId);
        distributedLockService.executeWithLock(lockKey,
                () -> reservationService.cancelReservation(reservationId));
    }
}
