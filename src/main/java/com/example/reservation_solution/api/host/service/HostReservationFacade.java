package com.example.reservation_solution.api.host.service;

import com.example.reservation_solution.global.exception.LockAcquisitionException;
import com.example.reservation_solution.global.lock.LockExecutor;
import com.example.reservation_solution.global.lock.LockKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HostReservationFacade {

    private final HostReservationService hostReservationService;
    private final LockExecutor lockExecutor;

    public void cancelReservation(Long reservationId, String hostEmail) {
        Long scheduleId = hostReservationService.getScheduleIdByReservationId(reservationId);
        String lockKey = LockKeyGenerator.schedule(scheduleId);
        try {
            lockExecutor.executeWithLock(lockKey,
                    () -> hostReservationService.cancelReservation(reservationId, hostEmail));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw LockAcquisitionException.interrupted();
        }
    }
}
