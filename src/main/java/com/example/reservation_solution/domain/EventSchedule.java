package com.example.reservation_solution.domain;

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

    public void assignEvent(Event event) {
        this.event = event;
    }

    public void incrementReservedCount() {
        if (this.reservedCount >= this.maxCapacity) {
            throw new IllegalStateException("이미 정원이 가득 찼습니다.");
        }
        this.reservedCount++;
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
