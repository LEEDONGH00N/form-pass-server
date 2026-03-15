package com.example.reservation_solution.api.event.repository;

import com.example.reservation_solution.api.auth.domain.Host;
import com.example.reservation_solution.api.auth.domain.Role;
import com.example.reservation_solution.api.auth.repository.HostRepository;
import com.example.reservation_solution.api.event.domain.Event;
import com.example.reservation_solution.api.event.domain.EventSchedule;
import com.example.reservation_solution.global.config.JpaAuditingConfig;
import com.example.reservation_solution.global.config.QuerydslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class EventRepositoryImplTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private HostRepository hostRepository;

    private Event savedEvent;

    @BeforeEach
    void setUp() {
        Host host = Host.create("host@test.com", "password", "테스트호스트", Role.HOST);
        hostRepository.save(host);

        Event event = Event.builder()
                .host(host)
                .title("테스트 이벤트")
                .location("서울")
                .description("설명")
                .eventCode("EVT00001")
                .build();

        EventSchedule schedule1 = EventSchedule.create(
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 4, 1, 12, 0), 50);
        EventSchedule schedule2 = EventSchedule.create(
                LocalDateTime.of(2026, 4, 1, 14, 0),
                LocalDateTime.of(2026, 4, 1, 16, 0), 30);
        event.addSchedule(schedule1);
        event.addSchedule(schedule2);

        savedEvent = eventRepository.save(event);
    }

    @Test
    @DisplayName("이벤트 ID로 스케줄 포함 조회 시 fetch join이 동작한다")
    void findByIdWithSchedules_fetchJoin() {
        // when
        Optional<Event> result = eventRepository.findByIdWithSchedules(savedEvent.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSchedules()).hasSize(2);
        assertThat(result.get().getTitle()).isEqualTo("테스트 이벤트");
    }

    @Test
    @DisplayName("이벤트 코드로 스케줄 포함 조회 시 fetch join이 동작한다")
    void findByEventCodeWithSchedules_fetchJoin() {
        // when
        Optional<Event> result = eventRepository.findByEventCodeWithSchedules("EVT00001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSchedules()).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 이벤트 코드 조회 시 빈 결과를 반환한다")
    void findByEventCodeWithSchedules_shouldReturnEmpty_whenNotFound() {
        // when
        Optional<Event> result = eventRepository.findByEventCodeWithSchedules("NOTEXIST");

        // then
        assertThat(result).isEmpty();
    }
}
