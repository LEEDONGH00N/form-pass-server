package com.example.reservation_solution.global.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BusinessException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public static BadRequestException invalidInput(String field) {
        return new BadRequestException(String.format("잘못된 입력입니다: %s", field));
    }

    public static BadRequestException requiredFieldMissing(String field) {
        return new BadRequestException(String.format("필수 항목이 누락되었습니다: %s", field));
    }

    public static BadRequestException requiredAnswerMissing(String questionText) {
        return new BadRequestException(String.format("필수 질문에 답변해야 합니다: %s", questionText));
    }
}
