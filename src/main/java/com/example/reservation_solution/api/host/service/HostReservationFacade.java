package com.example.reservation_solution.api.host.service;

import com.example.reservation_solution.global.lock.DistributedLockService;
import com.example.reservation_solution.global.lock.LockKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HostReservationFacade {

    private final HostReservationService hostReservationService;
    private final DistributedLockService distributedLockService;

    public void cancelReservation(Long reservationId, String hostEmail) {
        Long scheduleId = hostReservationService.getScheduleIdByReservationId(reservationId);
        String lockKey = LockKeyGenerator.schedule(scheduleId);
        distributedLockService.executeWithLock(lockKey,
                () -> hostReservationService.cancelReservation(reservationId, hostEmail));
    }
}
