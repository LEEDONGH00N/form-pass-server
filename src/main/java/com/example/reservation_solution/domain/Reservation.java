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
    private boolean isCheckedIn = false;

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
        this.status = ReservationStatus.CANCELLED;
    }

    public void checkIn() {
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
