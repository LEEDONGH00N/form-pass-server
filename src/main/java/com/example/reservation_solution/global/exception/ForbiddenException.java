package com.example.reservation_solution.global.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public static ForbiddenException noPermission(String resource) {
        return new ForbiddenException(String.format("해당 %s에 대한 권한이 없습니다", resource));
    }

    public static ForbiddenException eventNotPublic() {
        return new ForbiddenException("비공개된 이벤트입니다");
    }

    public static ForbiddenException cannotModifyWithReservations() {
        return new ForbiddenException("예약된 답변이 존재하여 질문을 수정할 수 없습니다");
    }
}
