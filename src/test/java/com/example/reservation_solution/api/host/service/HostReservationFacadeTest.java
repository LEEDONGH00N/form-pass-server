package com.example.reservation_solution.api.host.service;

import com.example.reservation_solution.global.exception.LockAcquisitionException;
import com.example.reservation_solution.global.lock.LockExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HostReservationFacadeTest {

    @InjectMocks
    private HostReservationFacade hostReservationFacade;

    @Mock
    private HostReservationService hostReservationService;

    @Mock
    private LockExecutor lockExecutor;

    @Test
    @DisplayName("호스트 예약 취소 시 락을 획득한 후 서비스에 위임한다")
    void cancelReservation_shouldAcquireLockAndDelegate() throws InterruptedException {
        // given
        given(hostReservationService.getScheduleIdByReservationId(1L)).willReturn(5L);

        // when
        hostReservationFacade.cancelReservation(1L, "host@example.com");

        // then
        verify(lockExecutor).executeWithLock(eq("schedule:5"), any(Runnable.class));
    }

    @Test
    @DisplayName("호스트 예약 취소 중 InterruptedException 발생 시 LockAcquisitionException으로 변환한다")
    void cancelReservation_shouldThrowLockAcquisitionException_whenInterrupted() throws InterruptedException {
        // given
        given(hostReservationService.getScheduleIdByReservationId(1L)).willReturn(5L);
        doThrow(new InterruptedException()).when(lockExecutor)
                .executeWithLock(eq("schedule:5"), any(Runnable.class));

        // when & then
        assertThatThrownBy(() -> hostReservationFacade.cancelReservation(1L, "host@example.com"))
                .isInstanceOf(LockAcquisitionException.class);
    }
}
