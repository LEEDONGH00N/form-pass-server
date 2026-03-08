package com.example.reservation_solution.global.exception;

import org.springframework.http.HttpStatus;

public class LockAcquisitionException extends BusinessException {

    public LockAcquisitionException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }

    public static LockAcquisitionException timeout() {
        return new LockAcquisitionException("현재 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }

    public static LockAcquisitionException interrupted() {
        return new LockAcquisitionException("락 획득 중 인터럽트가 발생했습니다.");
    }
}
