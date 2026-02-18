package com.example.reservation_solution.global.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public static NotFoundException entityNotFound(String entityName) {
        return new NotFoundException(String.format("존재하지 않는 %s입니다", entityName));
    }

    public static NotFoundException entityNotFound(String entityName, Long id) {
        return new NotFoundException(String.format("존재하지 않는 %s입니다 (ID: %d)", entityName, id));
    }
}
