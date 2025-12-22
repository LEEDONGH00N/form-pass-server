package com.example.reservation_solution.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "events")
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true, nullable = false, length = 10)
    private String eventCode;

    @Column(nullable = false)
    private boolean isPublic = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventSchedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventImage> images = new ArrayList<>();

    @Builder
    public Event(Host host, String title, String location, String description, String eventCode) {
        this.host = host;
        this.title = title;
        this.location = location;
        this.description = description;
        this.eventCode = eventCode;
    }

    public void addSchedule(EventSchedule schedule) {
        this.schedules.add(schedule);
        schedule.assignEvent(this);
    }

    public void addQuestion(FormQuestion question) {
        this.questions.add(question);
        question.assignEvent(this);
    }

    public void addImage(EventImage image) {
        this.images.add(image);
        image.assignEvent(this);
    }

    public static Event create(Host host, String title, String location, String description) {
        return Event.builder()
                .host(host)
                .title(title)
                .location(location)
                .description(description)
                .build();
    }

    public void updateBasicInfo(String title, String location, String description) {
        this.title = title;
        this.location = location;
        this.description = description;
    }

    public void clearSchedules() {
        this.schedules.clear();
    }

    public void clearQuestions() {
        this.questions.clear();
    }

    public void clearImages() {
        this.images.clear();
    }

    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
