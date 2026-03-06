# API 명세

Base URL: `https://api.form-pass.life`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## 인증

- **방식**: JWT Bearer Token
- **헤더**: `Authorization: Bearer {accessToken}`
- **만료**: 30분 (1,800,000ms)
- **알고리즘**: HS256

| 접근 수준 | 경로 | 설명 |
|-----------|------|------|
| Public | `/api/auth/**` | 인증 불필요 |
| Public | `/api/events/**` | 인증 불필요 |
| Public | `/api/reservations/**` | 인증 불필요 |
| Public | `/actuator/**`, `/health` | 모니터링 |
| ROLE_HOST | `/api/host/**` | JWT 필수 |

---

## 1. Auth API

### POST `/api/auth/signup`

호스트 회원가입

**Request Body:**
```json
{
  "email": "host@example.com",     // @NotBlank @Email
  "password": "password123",       // @NotBlank
  "name": "홍길동"                  // @NotBlank
}
```

**Response:** `201 Created`

**에러:**
| 상태 | 조건 |
|------|------|
| 400 | 이미 존재하는 이메일 |
| 400 | 이메일 인증 미완료 |

---

### POST `/api/auth/login`

로그인 (JWT 발급)

**Request Body:**
```json
{
  "email": "host@example.com",     // @NotBlank @Email
  "password": "password123"        // @NotBlank
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "host@example.com"
}
```

**에러:**
| 상태 | 조건 |
|------|------|
| 400 | 존재하지 않는 이메일 |
| 400 | 비밀번호 불일치 |

---

### POST `/api/auth/email/send`

이메일 인증코드 발송 (6자리)

**Request Body:**
```json
{
  "email": "host@example.com"
}
```

**Response:** `200 OK` `"인증 코드가 전송되었습니다."`

---

### POST `/api/auth/email/verify`

인증코드 확인

**Request Body:**
```json
{
  "email": "host@example.com",
  "authCode": "123456"
}
```

**Response:** `200 OK` `"이메일 인증이 완료되었습니다."`

**에러:**
| 상태 | 조건 |
|------|------|
| 400 | 잘못된 인증 코드 |

---

## 2. Event API (Guest - Public)

### GET `/api/events`

공개 이벤트 목록 조회 (isPublic = true)

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "2026 봄 페스티벌",
    "location": "서울 올림픽공원",
    "images": ["https://s3.../image1.jpg"],
    "description": "봄 축제입니다",
    "eventCode": "ABC1234567",
    "isPublic": true,
    "schedules": [
      {
        "id": 1,
        "startTime": "2026-04-01T10:00:00",
        "endTime": "2026-04-01T18:00:00",
        "maxCapacity": 100,
        "reservedCount": 45
      }
    ],
    "questions": [
      {
        "id": 1,
        "questionText": "소속을 입력해주세요",
        "questionType": "TEXT",
        "isRequired": true
      }
    ]
  }
]
```

---

### GET `/api/events/{eventCode}`

이벤트 상세 조회 (이벤트 코드 기반)

**Path Parameter:** `eventCode` (String, 10자리)

**Response:** `200 OK` (EventResponse 단건)

**접근 규칙:**
- 공개 이벤트: 누구나 조회 가능
- 비공개 이벤트: 호스트 본인만 조회 가능 (JWT 필요)

**에러:**
| 상태 | 조건 |
|------|------|
| 400 | 존재하지 않는 이벤트 코드 |
| 403 | 비공개 이벤트에 비소유자 접근 |

---

## 3. Event API (Host - ROLE_HOST)

### POST `/api/host/events`

이벤트 생성

**Headers:** `Authorization: Bearer {token}`

**Request Body:**
```json
{
  "title": "2026 봄 페스티벌",          // @NotBlank
  "location": "서울 올림픽공원",         // @NotBlank
  "images": ["https://s3.../img.jpg"],
  "description": "봄 축제입니다",
  "schedules": [                         // @NotEmpty @Valid
    {
      "startTime": "2026-04-01T10:00:00",  // @NotNull
      "endTime": "2026-04-01T18:00:00",    // @NotNull
      "maxCapacity": 100                    // @NotNull
    }
  ],
  "questions": [                         // @Valid
    {
      "questionText": "소속을 입력해주세요",  // @NotBlank
      "questionType": "TEXT",                // @NotNull (TEXT|CHECKBOX|RADIO)
      "isRequired": true                     // @NotNull
    }
  ]
}
```

**Response:** `201 Created` (EventResponse)

---

### GET `/api/host/events`

내 이벤트 목록 조회

**Response:** `200 OK` (List\<EventResponse\>)

---

### GET `/api/host/events/{eventId}`

내 이벤트 상세 조회

**Response:** `200 OK` (EventResponse)

**에러:**
| 상태 | 조건 |
|------|------|
| 400 | 존재하지 않는 이벤트 |
| 403 | 본인 소유가 아닌 이벤트 |

---

### PUT `/api/host/events/{eventId}`

이벤트 수정 (전체 교체)

**Request Body:** CreateEventRequest와 동일 구조

**Response:** `200 OK` (EventResponse)

**에러:**
| 상태 | 조건 |
|------|------|
| 400 | 존재하지 않는 이벤트 |
| 403 | 본인 소유가 아닌 이벤트 |
| 400 | 이미 폼 응답이 존재하는 질문 수정 시도 |

---

### PATCH `/api/host/events/{eventId}/visibility`

공개/비공개 전환

**Request Body:**
```json
{
  "isPublic": true    // @NotNull
}
```

**Response:** `200 OK`

---

## 4. Reservation API (Guest - Public)

### POST `/api/reservations`

예약 생성

**Request Body:**
```json
{
  "scheduleId": 1,                    // @NotNull
  "guestName": "김철수",              // @NotBlank
  "guestPhoneNumber": "01012345678",  // @NotBlank @Pattern(^01[0-9]\d{7,8}$)
  "ticketCount": 2,                   // @NotNull @Min(1)
  "answers": [
    {
      "questionId": 1,
      "answerText": "한국대학교"
    }
  ]
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "qrToken": "550e8400-e29b-41d4-a716-446655440000",
  "guestName": "김철수",
  "guestPhoneNumber": "01012345678",
  "ticketCount": 2,
  "status": "CONFIRMED",
  "isCheckedIn": false,
  "eventTitle": "2026 봄 페스티벌",
  "eventLocation": "서울 올림픽공원",
  "schedule": {
    "id": 1,
    "startTime": "2026-04-01T10:00:00",
    "endTime": "2026-04-01T18:00:00",
    "maxCapacity": 100,
    "reservedCount": 47
  },
  "answers": [
    {
      "questionId": 1,
      "questionText": "소속을 입력해주세요",
      "answerText": "한국대학교"
    }
  ],
  "createdAt": "2026-03-06T14:30:00"
}
```

**에러:**
| 상태 | 조건 |
|------|------|
| 400 | 존재하지 않는 스케줄 |
| 409 | 잔여 좌석 부족 (capacity 초과) |
| 409 | 동일 스케줄 + 전화번호 중복 예약 |
| 400 | 필수 질문 미응답 |

---

### GET `/api/reservations/{id}`

예약 상세 조회

**Response:** `200 OK` (ReservationResponse)

---

### GET `/api/reservations/qr/{qrToken}`

QR 토큰으로 예약 조회

**Response:** `200 OK` (ReservationResponse)

---

### DELETE `/api/reservations/{id}`

예약 취소

**Response:** `204 No Content`

**에러:**
| 상태 | 조건 |
|------|------|
| 400 | 존재하지 않는 예약 |
| 409 | 이미 취소된 예약 |
| 409 | 이미 체크인된 예약 |

---

### POST `/api/reservations/lookup`

이름 + 전화번호로 내 예약 조회

**Request Body:**
```json
{
  "guestName": "김철수",
  "guestPhoneNumber": "01012345678"
}
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "qrToken": "550e8400-...",
    "eventTitle": "2026 봄 페스티벌",
    "eventLocation": "서울 올림픽공원",
    "guestName": "김철수",
    "ticketCount": 2,
    "scheduleStartTime": "2026-04-01T10:00:00",
    "scheduleEndTime": "2026-04-01T18:00:00",
    "isCheckedIn": false,
    "createdAt": "2026-03-06T14:30:00"
  }
]
```

---

## 5. Host Reservation API (ROLE_HOST)

### GET `/api/host/events/{eventId}/dashboard`

대시보드 통계

**Response:** `200 OK`
```json
{
  "totalSeats": 500,
  "reservedCount": 234,
  "reservationRate": 46.8,
  "checkedInCount": 120,
  "availableSeats": 266
}
```

---

### GET `/api/host/events/{eventId}/reservations`

예약 목록 (페이징 + 필터링 + 검색)

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `scheduleId` | Long | X | - | 특정 스케줄 필터 |
| `searchKeyword` | String | X | - | 이름/전화번호 검색 |
| `page` | int | X | 0 | 페이지 번호 |
| `size` | int | X | 20 | 페이지 크기 |
| `sort` | String | X | createdAt,DESC | 정렬 |

**Response:** `200 OK` (Page\<ReservationListResponse\>)
```json
{
  "content": [
    {
      "id": 1,
      "guestName": "김철수",
      "guestPhoneNumber": "01012345678",
      "ticketCount": 2,
      "status": "CONFIRMED",
      "isCheckedIn": false,
      "createdAt": "2026-03-06T14:30:00",
      "scheduleName": "2026-04-01T10:00 ~ 2026-04-01T18:00"
    }
  ],
  "totalElements": 234,
  "totalPages": 12,
  "size": 20,
  "number": 0
}
```

---

### GET `/api/host/reservations/{reservationId}`

예약 상세 조회 (호스트)

**Response:** `200 OK` (ReservationResponse)

---

### PATCH `/api/host/reservations/{reservationId}/cancel`

호스트의 예약 취소

**Response:** `204 No Content`

---

### POST `/api/host/checkin`

QR 체크인

**Request Body:**
```json
{
  "qrToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** `200 OK`
```json
{
  "message": "체크인이 완료되었습니다.",
  "guestName": "김철수",
  "ticketCount": 2
}
```

**에러:**
| 상태 | 조건 |
|------|------|
| 400 | 존재하지 않는 QR 토큰 |
| 409 | 이미 체크인 완료 |
| 409 | 취소된 예약 |
| 403 | 본인 소유 이벤트가 아님 |

---

### PATCH `/api/host/reservations/{reservationId}/checkin`

수동 체크인

**Response:** `200 OK` (CheckinResponse)

---

### GET `/api/host/events/{eventId}/schedules-status`

스케줄별 현황

**Response:** `200 OK`
```json
[
  {
    "scheduleId": 1,
    "startTime": "2026-04-01T10:00",
    "endTime": "2026-04-01T18:00",
    "maxCapacity": 100,
    "currentCount": 45,
    "reservations": [
      {
        "id": 1,
        "guestName": "김철수",
        "guestPhoneNumber": "01012345678",
        "ticketCount": 2,
        "isCheckedIn": false
      }
    ]
  }
]
```

---

## 6. Image API (ROLE_HOST)

### POST `/api/host/s3/presigned-url`

S3 Presigned URL 발급

**Request Body:**
```json
{
  "fileName": "event-banner.jpg",
  "contentType": "image/jpeg"
}
```

**Response:** `200 OK`
```json
{
  "presignedUrl": "https://form-pass-images.s3.ap-northeast-2.amazonaws.com/...",
  "fileUrl": "https://form-pass-images.s3.ap-northeast-2.amazonaws.com/event-banner.jpg"
}
```

---

## 7. Monitoring (Public)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/health` | 서버 상태 (status, timestamp) |
| GET | `/actuator/health` | 상세 헬스체크 (DB, Disk) |
| GET | `/actuator/prometheus` | Prometheus 메트릭 |
| GET | `/actuator/metrics` | 전체 메트릭 목록 |

---

## 공통 에러 응답 형식

```json
{
  "status": 400,
  "message": "존재하지 않는 이벤트입니다"
}
```

| HTTP 상태 | 발생 조건 |
|-----------|----------|
| 400 Bad Request | 유효성 검증 실패, 존재하지 않는 리소스 |
| 401 Unauthorized | JWT 없음 또는 만료 |
| 403 Forbidden | 권한 없음 (소유권 불일치) |
| 409 Conflict | 중복 예약, 좌석 부족, 이미 취소/체크인 |
| 500 Internal Server Error | 서버 내부 오류 |
