package com.example.reservation_solution.api.auth.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HostRepositoryImpl implements HostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
}
