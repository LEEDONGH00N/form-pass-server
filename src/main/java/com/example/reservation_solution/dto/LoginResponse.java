package com.example.reservation_solution.dto;

public record LoginResponse(
    String accessToken,
    String email
) {
    public static LoginResponse of(String accessToken, String email) {
        return new LoginResponse(accessToken, email);
    }
}
