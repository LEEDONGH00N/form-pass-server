package com.example.reservation_solution.api.event.repository;

import com.example.reservation_solution.api.event.domain.Event;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static com.example.reservation_solution.api.event.domain.QEvent.event;
import static com.example.reservation_solution.api.event.domain.QEventSchedule.eventSchedule;

@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Event> findByIdWithSchedules(Long eventId) {
        Event result = queryFactory
                .selectFrom(event)
                .leftJoin(event.schedules, eventSchedule).fetchJoin()
                .where(event.id.eq(eventId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Event> findByEventCodeWithSchedules(String eventCode) {
        Event result = queryFactory
                .selectFrom(event)
                .leftJoin(event.schedules, eventSchedule).fetchJoin()
                .where(event.eventCode.eq(eventCode))
                .fetchOne();
        return Optional.ofNullable(result);
    }
}
