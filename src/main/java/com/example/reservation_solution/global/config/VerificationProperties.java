package com.example.reservation_solution.global.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "verification")
public class VerificationProperties {

    private final int codeExpirationMinutes;
    private final int signupWindowMinutes;
    private final int cacheMaxSize;
}
