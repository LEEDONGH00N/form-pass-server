package com.example.reservation_solution.global.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    public static UnauthorizedException tokenExpired() {
        return new UnauthorizedException("인증이 만료되었습니다. 다시 로그인해주세요");
    }

    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException("유효하지 않은 인증입니다");
    }
}
