# ERD (Entity Relationship Diagram)

---

## 관계도

```
┌──────────┐       ┌──────────────┐       ┌─────────────────┐
│  hosts   │       │   events     │       │ event_schedules  │
├──────────┤       ├──────────────┤       ├─────────────────┤
│ id (PK)  │──1:N─▶│ id (PK)      │──1:N─▶│ id (PK)         │
│ email    │       │ host_id (FK) │       │ event_id (FK)   │
│ password │       │ title        │       │ startTime       │
│ name     │       │ location     │       │ endTime         │
│ role     │       │ description  │       │ maxCapacity     │
└──────────┘       │ eventCode    │       │ reservedCount   │
                   │ isPublic     │       └────────┬────────┘
                   └──────┬───────┘                │
                          │                        │ 1:N
                          │ 1:N                    ▼
                   ┌──────┴───────┐       ┌─────────────────┐
                   │form_questions│       │  reservations   │
                   ├──────────────┤       ├─────────────────┤
                   │ id (PK)      │       │ id (PK)         │
                   │ event_id(FK) │       │ schedule_id(FK) │
                   │ questionText │       │ guestName       │
                   │ questionType │       │ guestPhone(AES) │
                   │ isRequired   │       │ ticketCount     │
                   └──────┬───────┘       │ qrToken (UUID)  │
                          │               │ isCheckedIn     │
                          │               │ status          │
                          │               └────────┬────────┘
                          │                        │ 1:N
                          │               ┌────────┴────────┐
                          │               │  form_answers   │
                          └──────────────▶├─────────────────┤
                               N:1        │ id (PK)         │
                                          │ reservation_id  │
                                          │ question_id(FK) │
                                          │ answerText      │
                                          └─────────────────┘

┌──────────────┐
│ event_images │
├──────────────┤
│ id (PK)      │
│ event_id(FK) │  ◀── events 1:N
│ imageUrl     │
│ orderIndex   │
└──────────────┘
```

---

## 테이블 스키마

### hosts

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| email | VARCHAR | NOT NULL, UNIQUE | 로그인 이메일 |
| password | VARCHAR | NOT NULL | BCrypt 해시 |
| name | VARCHAR | NOT NULL | 호스트 이름 |
| role | VARCHAR | NOT NULL | Enum(HOST) |
| created_at | DATETIME | NOT NULL, 수정불가 | JPA Auditing |
| updated_at | DATETIME | NOT NULL | JPA Auditing |

---

### events

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| host_id | BIGINT | FK → hosts.id, NOT NULL | 소유 호스트 |
| title | VARCHAR | NOT NULL | 이벤트명 |
| location | VARCHAR | NOT NULL | 장소 |
| description | TEXT | | 상세 설명 |
| event_code | VARCHAR(10) | NOT NULL, UNIQUE | 10자리 고유 코드 |
| is_public | BOOLEAN | | 공개 여부 (기본 false) |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**Cascade:** schedules, questions, images → ALL + orphanRemoval

---

### event_schedules

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| event_id | BIGINT | FK → events.id, NOT NULL | |
| start_time | DATETIME | NOT NULL | 시작 시간 |
| end_time | DATETIME | NOT NULL | 종료 시간 |
| max_capacity | INT | NOT NULL | 최대 수용 인원 |
| reserved_count | INT | NOT NULL, DEFAULT 0 | 현재 예약 수 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**Lock:** `PESSIMISTIC_WRITE` (findByIdWithLock)

---

### form_questions

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| event_id | BIGINT | FK → events.id, NOT NULL | |
| question_text | VARCHAR | NOT NULL | 질문 내용 |
| question_type | VARCHAR | NOT NULL | Enum(TEXT, CHECKBOX, RADIO) |
| is_required | BOOLEAN | NOT NULL | 필수 응답 여부 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

### reservations

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| event_schedule_id | BIGINT | FK → event_schedules.id, NOT NULL | |
| guest_name | VARCHAR | | 게스트 이름 |
| guest_phone_number | VARCHAR | | AES-256 암호화 저장 |
| ticket_count | INT | NOT NULL, DEFAULT 1 | 티켓 수량 |
| qr_token | VARCHAR | NOT NULL, UNIQUE | UUID 기반 체크인 토큰 |
| is_checked_in | BOOLEAN | NOT NULL, DEFAULT false | 체크인 여부 |
| status | VARCHAR | NOT NULL | Enum(CONFIRMED, CANCELLED) |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**Cascade:** formAnswers → ALL + orphanRemoval

---

### form_answers

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| reservation_id | BIGINT | FK → reservations.id, NOT NULL | |
| form_question_id | BIGINT | FK → form_questions.id, NOT NULL | |
| answer_text | VARCHAR | NOT NULL | 응답 내용 |

---

### event_images

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| event_id | BIGINT | FK → events.id, NOT NULL | |
| image_url | VARCHAR | NOT NULL | S3 이미지 URL |
| order_index | INT | NOT NULL | 정렬 순서 |

---

## Enum 정의

### Role
| 값 | 설명 |
|-----|------|
| `HOST` | 이벤트 주최자 |

### ReservationStatus
| 값 | 설명 |
|-----|------|
| `CONFIRMED` | 예약 확정 |
| `CANCELLED` | 예약 취소 |

### QuestionType
| 값 | 설명 |
|-----|------|
| `TEXT` | 텍스트 입력 |
| `CHECKBOX` | 체크박스 |
| `RADIO` | 라디오 버튼 |

---

## 관계 요약

| 부모 | 자식 | 관계 | Cascade | 설명 |
|------|------|------|---------|------|
| hosts | events | 1:N | - | 호스트가 이벤트 소유 |
| events | event_schedules | 1:N | ALL + orphanRemoval | 이벤트 삭제 시 스케줄 삭제 |
| events | form_questions | 1:N | ALL + orphanRemoval | 이벤트 삭제 시 질문 삭제 |
| events | event_images | 1:N | ALL + orphanRemoval | 이벤트 삭제 시 이미지 삭제 |
| event_schedules | reservations | 1:N | - | 스케줄별 예약 관리 |
| reservations | form_answers | 1:N | ALL + orphanRemoval | 예약 삭제 시 응답 삭제 |
| form_questions | form_answers | 1:N | - | 질문-응답 매핑 |

---

## Fetch 전략

모든 `@ManyToOne`, `@OneToOne` 관계는 `FetchType.LAZY`를 사용합니다.

**EntityGraph 사용처:**
- `ReservationRepository.findByIdWithDetails` → eventSchedule, formAnswers, formAnswers.formQuestion
- `ReservationRepository.findByGuestNameAndGuestPhoneNumberAndStatus` → eventSchedule, eventSchedule.event, formAnswers, formAnswers.formQuestion

**Fetch Join 사용처:**
- `EventRepository.findByIdWithDetails` → LEFT JOIN FETCH schedules
- `EventRepository.findByEventCodeWithDetails` → LEFT JOIN FETCH schedules
