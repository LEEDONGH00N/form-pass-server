package com.example.reservation_solution.api.event.domain;

import com.example.reservation_solution.global.common.domain.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "event_schedules")
public class EventSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private Integer reservedCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Builder
    public EventSchedule(LocalDateTime startTime, LocalDateTime endTime, Integer maxCapacity) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCapacity = maxCapacity;
        this.reservedCount = 0;
    }

    public static EventSchedule create(LocalDateTime startTime, LocalDateTime endTime, Integer maxCapacity) {
        return EventSchedule.builder()
                .startTime(startTime)
                .endTime(endTime)
                .maxCapacity(maxCapacity)
                .build();
    }

    public void assignEvent(Event event) {
        this.event = event;
    }

    public void incrementReservedCount() {
        incrementReservedCount(1);
    }

    public void incrementReservedCount(int count) {
        if (this.reservedCount + count > this.maxCapacity) {
            throw new IllegalStateException("예약 가능 좌석을 초과했습니다. (요청: " + count + ", 잔여: " + getAvailableSeats() + ")");
        }
        this.reservedCount += count;
    }

    public void decrementReservedCount(int count) {
        if (this.reservedCount < count) {
            throw new IllegalStateException("예약 카운트가 음수가 될 수 없습니다.");
        }
        this.reservedCount -= count;
    }

    public boolean isFull() {
        return this.reservedCount >= this.maxCapacity;
    }

    public int getAvailableSeats() {
        return this.maxCapacity - this.reservedCount;
    }
}
