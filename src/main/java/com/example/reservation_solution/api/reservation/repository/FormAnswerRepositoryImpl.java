package com.example.reservation_solution.api.reservation.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FormAnswerRepositoryImpl implements FormAnswerRepositoryCustom {

    private final JPAQueryFactory queryFactory;
}
