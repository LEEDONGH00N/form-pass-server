package com.example.reservation_solution.api.event.service;

import com.example.reservation_solution.api.auth.domain.Host;
import com.example.reservation_solution.api.event.domain.Event;
import com.example.reservation_solution.api.event.dto.*;
import com.example.reservation_solution.api.event.repository.EventRepository;
import com.example.reservation_solution.api.auth.repository.HostRepository;
import com.example.reservation_solution.api.reservation.repository.FormAnswerRepository;
import com.example.reservation_solution.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private HostRepository hostRepository;

    @Mock
    private FormAnswerRepository formAnswerRepository;

    @Test
    @DisplayName("이벤트 생성 성공")
    void createEvent_success() {
        // given
        Host host = TestFixtures.createHost();
        CreateEventRequest request = new CreateEventRequest(
                "테스트 이벤트", "서울", null, "설명",
                List.of(new ScheduleRequest(
                        LocalDateTime.of(2026, 4, 1, 10, 0),
                        LocalDateTime.of(2026, 4, 1, 12, 0), 50)),
                null
        );

        given(hostRepository.findByEmail("host@example.com")).willReturn(Optional.of(host));
        given(eventRepository.existsByEventCode(anyString())).willReturn(false);
        given(eventRepository.save(any(Event.class))).willAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        // when
        EventResponse response = eventService.createEvent("host@example.com", request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("테스트 이벤트");
        assertThat(response.getSchedules()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 호스트로 이벤트 생성 시 예외가 발생한다")
    void createEvent_shouldThrowException_whenHostNotFound() {
        // given
        CreateEventRequest request = new CreateEventRequest(
                "테스트", "서울", null, "설명",
                List.of(new ScheduleRequest(
                        LocalDateTime.of(2026, 4, 1, 10, 0),
                        LocalDateTime.of(2026, 4, 1, 12, 0), 50)),
                null
        );
        given(hostRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventService.createEvent("unknown@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 호스트입니다");
    }

    @Test
    @DisplayName("내 이벤트 목록 조회 성공")
    void getMyEvents_success() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);

        given(hostRepository.findByEmail("host@example.com")).willReturn(Optional.of(host));
        given(eventRepository.findByHost(host)).willReturn(List.of(event));

        // when
        List<EventResponse> responses = eventService.getMyEvents("host@example.com");

        // then
        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("이벤트 상세 조회 성공")
    void getEventDetail_success() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);

        given(eventRepository.findByIdWithSchedules(1L)).willReturn(Optional.of(event));

        // when
        EventResponse response = eventService.getEventDetail(1L, "host@example.com");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("테스트 이벤트");
    }

    @Test
    @DisplayName("이벤트 상세 조회 시 권한이 없으면 예외가 발생한다")
    void getEventDetail_shouldThrowException_whenUnauthorized() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);

        given(eventRepository.findByIdWithSchedules(1L)).willReturn(Optional.of(event));

        // when & then
        assertThatThrownBy(() -> eventService.getEventDetail(1L, "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 이벤트에 대한 권한이 없습니다");
    }

    @Test
    @DisplayName("이벤트 수정 시 폼 답변이 존재하면 질문 수정 불가")
    void updateEvent_shouldThrowException_whenFormAnswersExist() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);
        EventUpdateRequest request = new EventUpdateRequest(
                "수정 이벤트", "부산", null, "수정 설명",
                List.of(new ScheduleRequest(
                        LocalDateTime.of(2026, 4, 1, 10, 0),
                        LocalDateTime.of(2026, 4, 1, 12, 0), 50)),
                List.of(new QuestionRequest("새 질문", com.example.reservation_solution.api.event.domain.QuestionType.TEXT, true))
        );

        given(eventRepository.findByIdWithSchedules(1L)).willReturn(Optional.of(event));
        given(formAnswerRepository.existsByEventId(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> eventService.updateEvent(1L, "host@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약된 답변이 존재하여 질문을 수정할 수 없습니다.");
    }

    @Test
    @DisplayName("공개 이벤트 코드로 조회 성공")
    void getEvent_shouldReturnPublicEvent() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);
        ReflectionTestUtils.setField(event, "isPublic", true);

        given(eventRepository.findByEventCodeWithSchedules("EVT12345")).willReturn(Optional.of(event));

        // when
        EventResponse response = eventService.getEvent("EVT12345", null);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("비공개 이벤트를 비소유자가 조회 시 예외가 발생한다")
    void getEvent_shouldThrowException_whenPrivateEventAccessedByNonOwner() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);
        ReflectionTestUtils.setField(event, "isPublic", false);

        given(eventRepository.findByEventCodeWithSchedules("EVT12345")).willReturn(Optional.of(event));

        // when & then
        assertThatThrownBy(() -> eventService.getEvent("EVT12345", "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("비공개된 이벤트입니다");
    }

    @Test
    @DisplayName("이벤트 공개여부 변경 성공")
    void updateVisibility_success() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);

        given(eventRepository.findById(1L)).willReturn(Optional.of(event));

        // when
        eventService.updateVisibility(1L, "host@example.com", true);

        // then
        assertThat(event.getIsPublic()).isTrue();
    }

    @Test
    @DisplayName("권한 없는 사용자가 공개여부 변경 시 예외가 발생한다")
    void updateVisibility_shouldThrowException_whenUnauthorized() {
        // given
        Host host = TestFixtures.createHost();
        Event event = TestFixtures.createEvent(host);

        given(eventRepository.findById(1L)).willReturn(Optional.of(event));

        // when & then
        assertThatThrownBy(() -> eventService.updateVisibility(1L, "other@example.com", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 이벤트에 대한 권한이 없습니다");
    }
}
