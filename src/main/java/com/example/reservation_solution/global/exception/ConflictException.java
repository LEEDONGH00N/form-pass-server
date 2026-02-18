package com.example.reservation_solution.global.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public static ConflictException duplicateReservation() {
        return new ConflictException("이미 해당 스케줄에 예약하셨습니다");
    }

    public static ConflictException capacityExceeded(int requested, int available) {
        return new ConflictException(
                String.format("예약 가능 좌석을 초과했습니다 (요청: %d, 잔여: %d)", requested, available)
        );
    }

    public static ConflictException alreadyCheckedIn() {
        return new ConflictException("이미 체크인된 예약입니다");
    }

    public static ConflictException alreadyCancelled() {
        return new ConflictException("이미 취소된 예약입니다");
    }
}
