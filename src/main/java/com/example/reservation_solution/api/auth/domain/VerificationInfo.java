package com.example.reservation_solution.api.auth.domain;

import java.time.LocalDateTime;

public record VerificationInfo(
        String code,
        LocalDateTime expiryTime,
        boolean verified
) {}
