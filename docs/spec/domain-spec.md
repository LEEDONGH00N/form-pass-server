# 도메인 상세

---

## 1. 엔티티

### Host

```java
@Entity @Table(name = "hosts")
public class Host extends BaseTimeEntity {
    @Id @GeneratedValue(IDENTITY) Long id;
    @Column(unique = true, nullable = false) String email;
    @Column(nullable = false) String password;          // BCrypt
    @Column(nullable = false) String name;
    @Enumerated(STRING) @Column(nullable = false) Role role;
}
```

**팩토리 메서드:**
```java
Host.create(String email, String password, String name, Role role)
```

---

### Event

```java
@Entity @Table(name = "events")
public class Event extends BaseTimeEntity {
    @Id @GeneratedValue(IDENTITY) Long id;
    @Column(nullable = false) String title;
    @Column(nullable = false) String location;
    @Column(columnDefinition = "TEXT") String description;
    @Column(unique = true, nullable = false, length = 10) String eventCode;
    Boolean isPublic;

    @ManyToOne(LAZY) Host host;

    @OneToMany(mappedBy = "event", cascade = ALL, orphanRemoval = true)
    List<EventSchedule> schedules;

    @OneToMany(mappedBy = "event", cascade = ALL, orphanRemoval = true)
    List<FormQuestion> questions;

    @OneToMany(mappedBy = "event", cascade = ALL, orphanRemoval = true)
    List<EventImage> images;
}
```

**팩토리 메서드:**
```java
Event.create(String title, String location, String description, String eventCode, Host host)
```

**주요 메서드:**
- `updateBasicInfo(title, location, description)` - 기본 정보 수정
- `clearSchedules()` / `clearQuestions()` - 하위 엔티티 초기화 (전체 교체용)
- `addSchedule(EventSchedule)` / `addQuestion(FormQuestion)` / `addImage(EventImage)`
- `updateVisibility(Boolean isPublic)`

---

### EventSchedule

```java
@Entity @Table(name = "event_schedules")
public class EventSchedule extends BaseTimeEntity {
    @Id @GeneratedValue(IDENTITY) Long id;
    @Column(nullable = false) LocalDateTime startTime;
    @Column(nullable = false) LocalDateTime endTime;
    @Column(nullable = false) Integer maxCapacity;
    @Column(nullable = false) Integer reservedCount = 0;

    @ManyToOne(LAZY) Event event;
}
```

**팩토리 메서드:**
```java
EventSchedule.create(LocalDateTime startTime, LocalDateTime endTime, Integer maxCapacity, Event event)
```

**주요 메서드:**
- `incrementReservedCount(int count)` - 예약 수 증가 (capacity 초과 시 IllegalStateException)
- `decrementReservedCount(int count)` - 예약 수 감소
- `isFull()` → Boolean
- `getAvailableSeats()` → int

---

### Reservation

```java
@Entity @Table(name = "reservations")
public class Reservation extends BaseTimeEntity {
    @Id @GeneratedValue(IDENTITY) Long id;
    @ManyToOne(LAZY) EventSchedule eventSchedule;
    String guestName;
    String guestPhoneNumber;                              // AES-256 암호화
    @Column(nullable = false) Integer ticketCount = 1;
    @Column(unique = true, nullable = false) String qrToken;  // UUID
    @Column(nullable = false) Boolean isCheckedIn = false;
    @Enumerated(STRING) @Column(nullable = false) ReservationStatus status;

    @OneToMany(mappedBy = "reservation", cascade = ALL, orphanRemoval = true)
    List<FormAnswer> formAnswers;
}
```

**팩토리 메서드:**
```java
Reservation.create(EventSchedule schedule, String guestName, String guestPhoneNumber, Integer ticketCount)
// qrToken = UUID.randomUUID(), status = CONFIRMED, isCheckedIn = false
```

**주요 메서드:**
- `cancel()` - 상태를 CANCELLED로 변경
- `checkIn()` - isCheckedIn을 true로 변경
- `validateCancellable()` - 취소 가능 여부 검증 (취소됨/체크인됨 시 예외)
- `validateCheckInPossible()` - 체크인 가능 여부 검증
- `addFormAnswer(FormAnswer)`

---

### FormQuestion

```java
@Entity @Table(name = "form_questions")
public class FormQuestion extends BaseTimeEntity {
    @Id @GeneratedValue(IDENTITY) Long id;
    @Column(nullable = false) String questionText;
    @Enumerated(STRING) @Column(nullable = false) QuestionType questionType;
    @Column(nullable = false) Boolean isRequired;

    @ManyToOne(LAZY) Event event;
}
```

**팩토리 메서드:**
```java
FormQuestion.create(String questionText, QuestionType questionType, Boolean isRequired, Event event)
```

---

### FormAnswer

```java
@Entity @Table(name = "form_answers")
public class FormAnswer {
    @Id @GeneratedValue(IDENTITY) Long id;
    @ManyToOne(LAZY) Reservation reservation;
    @ManyToOne(LAZY) FormQuestion formQuestion;
    @Column(nullable = false) String answerText;
}
```

**팩토리 메서드:**
```java
FormAnswer.create(Reservation reservation, FormQuestion formQuestion, String answerText)
```

---

### EventImage

```java
@Entity @Table(name = "event_images")
public class EventImage {
    @Id @GeneratedValue(IDENTITY) Long id;
    @Column(nullable = false) String imageUrl;
    @Column(nullable = false) Integer orderIndex;

    @ManyToOne(LAZY) Event event;
}
```

**팩토리 메서드:**
```java
EventImage.create(String imageUrl, Integer orderIndex, Event event)
```

---

### BaseTimeEntity (공통 상위 클래스)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate @Column(updatable = false) LocalDateTime createdAt;
    @LastModifiedDate LocalDateTime updatedAt;
}
```

---

## 2. Enum

| Enum | 값 | 위치 |
|------|----|------|
| `Role` | HOST | api.auth.domain |
| `ReservationStatus` | CONFIRMED, CANCELLED | api.reservation.domain |
| `QuestionType` | TEXT, CHECKBOX, RADIO | api.event.domain |

---

## 3. DTO

### Auth

| 클래스 | 타입 | 필드 |
|--------|------|------|
| `SignupRequest` | record | email(@NotBlank @Email), password(@NotBlank), name(@NotBlank) |
| `LoginRequest` | record | email(@NotBlank @Email), password(@NotBlank) |
| `LoginResponse` | record | accessToken, email |
| `TokenResponse` | record | accessToken |
| `EmailSendRequest` | record | email |
| `EmailVerifyRequest` | record | email, authCode |

### Event

| 클래스 | 타입 | 필드 |
|--------|------|------|
| `CreateEventRequest` | class | title(@NotBlank), location(@NotBlank), images(List\<String\>), description, schedules(@NotEmpty @Valid List), questions(@Valid List) |
| `EventUpdateRequest` | class | CreateEventRequest와 동일 구조 |
| `EventVisibilityRequest` | record | isPublic(@NotNull) |
| `ScheduleRequest` | class | startTime(@NotNull), endTime(@NotNull), maxCapacity(@NotNull) |
| `QuestionRequest` | class | questionText(@NotBlank), questionType(@NotNull), isRequired(@NotNull) |
| `EventResponse` | class | id, title, location, images, description, eventCode, isPublic, schedules, questions |
| `ScheduleResponse` | class | id, startTime, endTime, maxCapacity, reservedCount |
| `QuestionResponse` | class | id, questionText, questionType, isRequired |

### Reservation

| 클래스 | 타입 | 필드 |
|--------|------|------|
| `ReservationRequest` | class | scheduleId(@NotNull), guestName(@NotBlank), guestPhoneNumber(@NotBlank @Pattern), ticketCount(@NotNull @Min(1)), answers(List) |
| `ReservationResponse` | class | id, qrToken, guestName, guestPhoneNumber(복호화), ticketCount, status, isCheckedIn, eventTitle, eventLocation, schedule, answers, createdAt |
| `ReservationListResponse` | class | id, guestName, guestPhoneNumber, ticketCount, status, isCheckedIn, createdAt, scheduleName |
| `ReservationLookupRequest` | class | guestName, guestPhoneNumber |
| `ReservationLookupResponse` | class | id, qrToken, eventTitle, eventLocation, guestName, ticketCount, scheduleStartTime, scheduleEndTime, isCheckedIn, createdAt |
| `FormAnswerRequest` | record | questionId, answerText |
| `FormAnswerResponse` | class | questionId, questionText, answerText |

### Host

| 클래스 | 타입 | 필드 |
|--------|------|------|
| `DashboardResponse` | record | totalSeats, reservedCount, reservationRate(Double), checkedInCount, availableSeats |
| `CheckinRequest` | record | qrToken |
| `CheckinResponse` | record | message, guestName, ticketCount |
| `ScheduleStatusResponse` | class | scheduleId, startTime, endTime, maxCapacity, currentCount, reservations(List\<SimpleReservationDto\>) |
| `SimpleReservationDto` | class | id, guestName, guestPhoneNumber, ticketCount, isCheckedIn |

---

## 4. Repository 쿼리 메서드

### HostRepository

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `findByEmail(String)` | Optional\<Host\> | 이메일로 호스트 조회 |
| `existsByEmail(String)` | boolean | 이메일 중복 확인 |

### EventRepository

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `findByHost(Host)` | List\<Event\> | 호스트의 이벤트 목록 |
| `findByIdWithDetails(Long)` | Optional\<Event\> | Fetch Join (schedules) |
| `findByEventCodeWithDetails(String)` | Optional\<Event\> | 이벤트 코드로 조회 + Fetch Join |
| `existsByEventCode(String)` | boolean | 이벤트 코드 중복 확인 |

### EventScheduleRepository

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `findByIdWithLock(Long)` | Optional\<EventSchedule\> | **PESSIMISTIC_WRITE Lock** |

### FormQuestionRepository

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `findByEventIdOrderById(Long)` | List\<FormQuestion\> | 이벤트의 질문 목록 (ID 순) |

### ReservationRepository

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `findByQrToken(String)` | Optional\<Reservation\> | QR 토큰으로 조회 |
| `existsByEventScheduleIdAndGuestPhoneNumberAndStatus(...)` | boolean | 중복 예약 확인 |
| `findByEventScheduleIdAndStatus(Long, Status)` | List\<Reservation\> | 스케줄별 예약 목록 |
| `findByEventScheduleIdIn(List\<Long\>, Pageable)` | Page\<Reservation\> | 다중 스케줄 페이징 |
| `findByIdWithDetails(Long)` | Optional\<Reservation\> | EntityGraph (schedule, answers) |
| `findByGuestNameAndGuestPhoneNumberAndStatus(...)` | List\<Reservation\> | 게스트 조회 + EntityGraph |
| `findByEventScheduleIdAndKeyword(Long, String, Pageable)` | Page\<Reservation\> | 스케줄 + 키워드 검색 |
| `findByEventScheduleIdInAndKeyword(List, String, Pageable)` | Page\<Reservation\> | 다중 스케줄 + 키워드 검색 |

### FormAnswerRepository

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `existsByEventId(Long)` | boolean | 이벤트에 폼 응답 존재 여부 (질문 수정 가능 판단) |
