package com.example.reservation_solution.dto;

/**
 * 로그인 성공 응답 DTO
 * JWT 토큰은 HttpOnly 쿠키로 전송되므로 응답 본문에는 성공 메시지만 포함
 */
public record LoginResponse(
    String message,
    String email
) {
    /**
     * 로그인 성공 응답 생성
     *
     * @param email 로그인한 사용자 이메일
     * @return LoginResponse 객체
     */
    public static LoginResponse success(String email) {
        return new LoginResponse("로그인이 성공적으로 완료되었습니다.", email);
    }
}
