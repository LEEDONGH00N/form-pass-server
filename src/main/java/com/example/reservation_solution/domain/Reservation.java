package com.example.reservation_solution.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservations")
//@Table(name = "reservations", indexes = {
//        @Index(name = "idx_reservation_schedule_phone_status",
//                columnList = "event_schedule_id, guest_phone_number")
//})
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_schedule_id", nullable = false)
    private EventSchedule eventSchedule;

    private String guestName;

    private String guestPhoneNumber;

    @Column(nullable = false)
    private Integer ticketCount = 1;

    @Column(nullable = false, unique = true)
    private String qrToken;

    @Column(nullable = false)
    private Boolean isCheckedIn = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormAnswer> formAnswers = new ArrayList<>();

    @Builder
    public Reservation(EventSchedule eventSchedule, String guestName, String guestPhoneNumber, Integer ticketCount) {
        this.eventSchedule = eventSchedule;
        this.guestName = guestName;
        this.guestPhoneNumber = guestPhoneNumber;
        this.ticketCount = ticketCount != null && ticketCount > 0 ? ticketCount : 1;
        this.qrToken = UUID.randomUUID().toString();
        this.isCheckedIn = false;
        this.status = ReservationStatus.CONFIRMED;
    }

    public void addFormAnswer(FormAnswer formAnswer) {
        this.formAnswers.add(formAnswer);
        formAnswer.assignReservation(this);
    }

    public void cancel() {
        validateCancellable();
        this.status = ReservationStatus.CANCELLED;
    }

    public void validateCancellable() {
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }
        if (this.isCheckedIn) {
            throw new IllegalStateException("이미 입장 완료된 티켓은 취소할 수 없습니다.");
        }
    }

    public void validateCheckInPossible() {
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("취소된 예약은 체크인할 수 없습니다.");
        }
        if (this.isCheckedIn) {
            throw new IllegalStateException("이미 체크인이 완료된 예약입니다.");
        }
    }

    public void checkIn() {
        validateCheckInPossible();
        this.isCheckedIn = true;
    }

    public static Reservation create(EventSchedule eventSchedule, String guestName, String guestPhoneNumber, Integer ticketCount) {
        return Reservation.builder()
                .eventSchedule(eventSchedule)
                .guestName(guestName)
                .guestPhoneNumber(guestPhoneNumber)
                .ticketCount(ticketCount)
                .build();
    }
}
