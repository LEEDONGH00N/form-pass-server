package com.example.reservation_solution.api.reservation.repository;

import com.example.reservation_solution.api.reservation.domain.Reservation;
import com.example.reservation_solution.api.reservation.domain.ReservationStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

import static com.example.reservation_solution.api.event.domain.QEventSchedule.eventSchedule;
import static com.example.reservation_solution.api.event.domain.QFormQuestion.formQuestion;
import static com.example.reservation_solution.api.reservation.domain.QFormAnswer.formAnswer;
import static com.example.reservation_solution.api.reservation.domain.QReservation.reservation;

@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Reservation> findByScheduleIdAndPhoneNumberAndStatus(Long scheduleId, String phoneNumber, ReservationStatus status) {
        Reservation result = queryFactory
                .selectFrom(reservation)
                .where(
                        reservation.eventSchedule.id.eq(scheduleId),
                        reservation.guestPhoneNumber.eq(phoneNumber),
                        reservation.status.eq(status)
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Reservation> findByIdWithDetails(Long id) {
        Reservation result = queryFactory
                .selectFrom(reservation)
                .leftJoin(reservation.eventSchedule, eventSchedule).fetchJoin()
                .leftJoin(reservation.formAnswers, formAnswer).fetchJoin()
                .leftJoin(formAnswer.formQuestion, formQuestion).fetchJoin()
                .where(reservation.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<Reservation> findByGuestInfoAndStatus(String guestName, String guestPhoneNumber, ReservationStatus status) {
        return queryFactory
                .selectFrom(reservation)
                .leftJoin(reservation.eventSchedule, eventSchedule).fetchJoin()
                .leftJoin(eventSchedule.event).fetchJoin()
                .leftJoin(reservation.formAnswers, formAnswer).fetchJoin()
                .leftJoin(formAnswer.formQuestion, formQuestion).fetchJoin()
                .where(
                        reservation.guestName.eq(guestName),
                        reservation.guestPhoneNumber.eq(guestPhoneNumber),
                        reservation.status.eq(status)
                )
                .orderBy(reservation.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<Reservation> searchReservations(Long scheduleId, List<Long> scheduleIds, String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // scheduleId 단일 조건 우선
        if (scheduleId != null) {
            builder.and(reservation.eventSchedule.id.eq(scheduleId));
        } else if (scheduleIds != null && !scheduleIds.isEmpty()) {
            builder.and(reservation.eventSchedule.id.in(scheduleIds));
        }

        // 키워드 검색 (이름 또는 전화번호)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmed = keyword.trim();
            builder.and(
                    reservation.guestName.containsIgnoreCase(trimmed)
                            .or(reservation.guestPhoneNumber.containsIgnoreCase(trimmed))
            );
        }

        List<Reservation> content = queryFactory
                .selectFrom(reservation)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(reservation.id.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(reservation.count())
                .from(reservation)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
