package com.example.reservation_solution.api.event.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FormQuestionRepositoryImpl implements FormQuestionRepositoryCustom {

    private final JPAQueryFactory queryFactory;
}
