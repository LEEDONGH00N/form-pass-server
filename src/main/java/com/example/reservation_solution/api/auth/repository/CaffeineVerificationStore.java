package com.example.reservation_solution.api.auth.repository;

import com.example.reservation_solution.api.auth.domain.VerificationInfo;
import com.example.reservation_solution.global.config.VerificationProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CaffeineVerificationStore implements VerificationStore {

    private final Cache<String, VerificationInfo> cache;

    public CaffeineVerificationStore(VerificationProperties properties) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(properties.getCodeExpirationMinutes(), TimeUnit.MINUTES)
                .maximumSize(properties.getCacheMaxSize())
                .build();
    }

    @Override
    public void save(String email, VerificationInfo info) {
        cache.put(email, info);
    }

    @Override
    public VerificationInfo find(String email) {
        return cache.getIfPresent(email);
    }

    @Override
    public void invalidate(String email) {
        cache.invalidate(email);
    }
}
