package com.example.reservation_solution.api.reservation.service;

import com.example.reservation_solution.api.reservation.dto.ReservationRequest;
import com.example.reservation_solution.api.reservation.dto.ReservationResponse;
import com.example.reservation_solution.global.exception.LockAcquisitionException;
import com.example.reservation_solution.global.lock.LockExecutor;
import com.example.reservation_solution.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationFacadeTest {

    @InjectMocks
    private ReservationFacade reservationFacade;

    @Mock
    private ReservationService reservationService;

    @Mock
    private LockExecutor lockExecutor;

    @Test
    @DisplayName("예약 생성 시 락을 획득한 후 서비스에 위임한다")
    @SuppressWarnings("unchecked")
    void createReservation_shouldAcquireLockAndDelegate() throws InterruptedException {
        // given
        ReservationRequest request = TestFixtures.createReservationRequest(1L);
        ReservationResponse mockResponse = org.mockito.Mockito.mock(ReservationResponse.class);

        given(lockExecutor.executeWithLock(eq("schedule:1"), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<ReservationResponse> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
        given(reservationService.createReservation(request)).willReturn(mockResponse);

        // when
        ReservationResponse response = reservationFacade.createReservation(request);

        // then
        assertThat(response).isEqualTo(mockResponse);
        verify(lockExecutor).executeWithLock(eq("schedule:1"), any(Supplier.class));
    }

    @Test
    @DisplayName("예약 생성 중 InterruptedException 발생 시 LockAcquisitionException으로 변환한다")
    @SuppressWarnings("unchecked")
    void createReservation_shouldThrowLockAcquisitionException_whenInterrupted() throws InterruptedException {
        // given
        ReservationRequest request = TestFixtures.createReservationRequest(1L);

        given(lockExecutor.executeWithLock(eq("schedule:1"), any(Supplier.class)))
                .willThrow(new InterruptedException());

        // when & then
        assertThatThrownBy(() -> reservationFacade.createReservation(request))
                .isInstanceOf(LockAcquisitionException.class);
    }

    @Test
    @DisplayName("예약 취소 시 락을 획득한 후 서비스에 위임한다")
    void cancelReservation_shouldAcquireLockAndDelegate() throws InterruptedException {
        // given
        given(reservationService.getScheduleIdByReservationId(1L)).willReturn(5L);

        // when
        reservationFacade.cancelReservation(1L);

        // then
        verify(lockExecutor).executeWithLock(eq("schedule:5"), any(Runnable.class));
    }

    @Test
    @DisplayName("예약 취소 중 InterruptedException 발생 시 LockAcquisitionException으로 변환한다")
    void cancelReservation_shouldThrowLockAcquisitionException_whenInterrupted() throws InterruptedException {
        // given
        given(reservationService.getScheduleIdByReservationId(1L)).willReturn(5L);
        doThrow(new InterruptedException()).when(lockExecutor)
                .executeWithLock(eq("schedule:5"), any(Runnable.class));

        // when & then
        assertThatThrownBy(() -> reservationFacade.cancelReservation(1L))
                .isInstanceOf(LockAcquisitionException.class);
    }
}
