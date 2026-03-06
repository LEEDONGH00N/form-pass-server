package com.example.reservation_solution.api.auth.repository;

import com.example.reservation_solution.api.auth.domain.VerificationInfo;

public interface VerificationStore {

    void save(String email, VerificationInfo info);

    VerificationInfo find(String email);

    void invalidate(String email);
}
