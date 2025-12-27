package com.example.reservation_solution.global.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import java.time.Duration;

public class CookieUtils {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String COOKIE_PATH = "/";
    private static final Duration COOKIE_MAX_AGE = Duration.ofSeconds(1800); // 30분

    public static ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)           // 자바스크립트에서 접근 불가 (XSS 방지)
                .secure(true)             // HTTPS 환경에서만 전송 (필수)
                .path(COOKIE_PATH)        // 모든 경로에서 쿠키 전송
                .maxAge(COOKIE_MAX_AGE)   // 30분 후 만료
                .sameSite("None")         // 서로 다른 도메인 간 요청 허용 (CORS)
                .build();
    }

    public static ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(0) // 즉시 만료시켜 브라우저에서 삭제
                .sameSite("None")
                .build();
    }
}