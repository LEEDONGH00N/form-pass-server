# Form PASS API 명세서

## 목차
- [인증 API](#인증-api)
- [이벤트 API](#이벤트-api)
- [예약 API](#예약-api)
- [호스트 관리 API](#호스트-관리-api)

---

## 인증 API

### 회원가입
`POST /api/auth/signup`

**Request**
```json
{
  "email": "host@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "email": "host@example.com",
  "name": "홍길동"
}
```

---

### 로그인
`POST /api/auth/login`

**Request**
```json
{
  "email": "host@example.com",
  "password": "password123"
}
```

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "host@example.com"
}
```

---

### 이메일 인증코드 발송
`POST /api/auth/email/send`

**Request**
```json
{
  "email": "user@example.com"
}
```

**Response (200 OK)**
```json
{
  "message": "인증 코드가 발송되었습니다."
}
```

---

### 이메일 인증코드 확인
`POST /api/auth/email/verify`

**Request**
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

**Response (200 OK)**
```json
{
  "verified": true
}
```

---

## 이벤트 API

### 이벤트 목록 조회 (Public)
`GET /api/events`

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "eventCode": "ABC123",
    "title": "2024 신년 콘서트",
    "location": "서울시 강남구",
    "description": "새해를 맞이하는 특별 콘서트",
    "isPublic": true,
    "schedules": [...],
    "questions": [...],
    "images": [...]
  }
]
```

---

### 이벤트 상세 조회 (Public)
`GET /api/events/{eventCode}`

**Response (200 OK)**
```json
{
  "id": 1,
  "eventCode": "ABC123",
  "title": "2024 신년 콘서트",
  "location": "서울시 강남구",
  "description": "새해를 맞이하는 특별 콘서트",
  "isPublic": true,
  "schedules": [
    {
      "id": 1,
      "startTime": "2024-01-20T14:00:00",
      "endTime": "2024-01-20T16:00:00",
      "maxCapacity": 100,
      "reservedCount": 25
    }
  ],
  "questions": [
    {
      "id": 1,
      "questionText": "소속 회사명",
      "questionType": "TEXT",
      "isRequired": true
    }
  ],
  "images": [
    {
      "id": 1,
      "imageUrl": "https://...",
      "displayOrder": 0
    }
  ]
}
```

---

## 예약 API

### 예약 생성
`POST /api/reservations`

**Request**
```json
{
  "scheduleId": 1,
  "guestName": "홍길동",
  "guestPhoneNumber": "01012345678",
  "ticketCount": 2,
  "answers": [
    {
      "questionId": 1,
      "answerText": "ABC 주식회사"
    }
  ]
}
```

**Response (201 Created)**
```json
{
  "id": 123,
  "qrToken": "550e8400-e29b-41d4-a716-446655440000",
  "guestName": "홍길동",
  "guestPhoneNumber": "01012345678",
  "ticketCount": 2,
  "status": "CONFIRMED",
  "isCheckedIn": false,
  "eventTitle": "2024 신년 콘서트",
  "eventLocation": "서울시 강남구",
  "schedule": {
    "id": 1,
    "startTime": "2024-01-20T14:00:00",
    "endTime": "2024-01-20T16:00:00",
    "maxCapacity": 100,
    "reservedCount": 27
  },
  "answers": [
    {
      "questionId": 1,
      "questionText": "소속 회사명",
      "answerText": "ABC 주식회사"
    }
  ],
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### 예약 조회
`GET /api/reservations/{id}`

**Response (200 OK)**
```json
{
  "id": 123,
  "qrToken": "550e8400-e29b-41d4-a716-446655440000",
  "guestName": "홍길동",
  "guestPhoneNumber": "01012345678",
  "ticketCount": 2,
  "status": "CONFIRMED",
  "isCheckedIn": false,
  "eventTitle": "2024 신년 콘서트",
  "eventLocation": "서울시 강남구",
  "schedule": {...},
  "answers": [...],
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### QR 토큰으로 예약 조회
`GET /api/reservations/qr/{qrToken}`

**Response (200 OK)**
```json
{
  "id": 123,
  "qrToken": "550e8400-e29b-41d4-a716-446655440000",
  "guestName": "홍길동",
  "guestPhoneNumber": "01012345678",
  "ticketCount": 2,
  "status": "CONFIRMED",
  "isCheckedIn": false,
  "eventTitle": "2024 신년 콘서트",
  "eventLocation": "서울시 강남구",
  "schedule": {...},
  "answers": [...],
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### 내 티켓 조회
`POST /api/reservations/lookup`

예약자가 이름과 전화번호로 자신의 예약 내역을 조회합니다.

**Request**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| guestName | String | O | 예약자 이름 |
| guestPhoneNumber | String | O | 예약자 전화번호 (하이픈 없이) |

```json
{
  "guestName": "홍길동",
  "guestPhoneNumber": "01012345678"
}
```

**Response (200 OK)**

| Field | Type | Description |
|-------|------|-------------|
| id | Long | 예약 ID |
| qrToken | String | QR 체크인용 토큰 |
| eventTitle | String | 이벤트 제목 |
| eventLocation | String | 이벤트 장소 |
| guestName | String | 예약자 이름 |
| ticketCount | Integer | 티켓 수량 |
| scheduleStartTime | DateTime | 스케줄 시작 시간 |
| scheduleEndTime | DateTime | 스케줄 종료 시간 |
| isCheckedIn | Boolean | 체크인 완료 여부 |
| createdAt | DateTime | 예약 생성일시 |

```json
[
  {
    "id": 123,
    "qrToken": "550e8400-e29b-41d4-a716-446655440000",
    "eventTitle": "2024 신년 콘서트",
    "eventLocation": "서울시 강남구 테헤란로 123",
    "guestName": "홍길동",
    "ticketCount": 2,
    "scheduleStartTime": "2024-01-20T14:00:00",
    "scheduleEndTime": "2024-01-20T16:00:00",
    "isCheckedIn": false,
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

---

### 예약 취소
`DELETE /api/reservations/{id}`

**Response (204 No Content)**

---

## 호스트 관리 API

> 모든 호스트 API는 `Authorization: Bearer {token}` 헤더가 필요합니다.

### 이벤트 생성
`POST /api/host/events`

**Request**
```json
{
  "title": "2024 신년 콘서트",
  "location": "서울시 강남구",
  "description": "새해를 맞이하는 특별 콘서트",
  "images": ["https://s3.../image1.jpg"],
  "schedules": [
    {
      "startTime": "2024-01-20T14:00:00",
      "endTime": "2024-01-20T16:00:00",
      "maxCapacity": 100
    }
  ],
  "questions": [
    {
      "questionText": "소속 회사명",
      "questionType": "TEXT",
      "isRequired": true
    }
  ]
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "eventCode": "ABC123",
  "title": "2024 신년 콘서트",
  ...
}
```

---

### 내 이벤트 목록 조회
`GET /api/host/events`

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "eventCode": "ABC123",
    "title": "2024 신년 콘서트",
    ...
  }
]
```

---

### 내 이벤트 상세 조회
`GET /api/host/events/{eventId}`

**Response (200 OK)**
```json
{
  "id": 1,
  "eventCode": "ABC123",
  "title": "2024 신년 콘서트",
  ...
}
```

---

### 이벤트 수정
`PUT /api/host/events/{eventId}`

**Request**
```json
{
  "title": "2024 신년 콘서트 (수정)",
  "location": "서울시 강남구",
  "description": "수정된 설명",
  "images": [...],
  "schedules": [...],
  "questions": [...]
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "eventCode": "ABC123",
  "title": "2024 신년 콘서트 (수정)",
  ...
}
```

---

### 이벤트 공개/비공개 설정
`PATCH /api/host/events/{eventId}/visibility`

**Request**
```json
{
  "isPublic": true
}
```

**Response (200 OK)**

---

### 대시보드 통계 조회
`GET /api/host/events/{eventId}/dashboard`

**Response (200 OK)**
```json
{
  "totalSeats": 100,
  "reservedCount": 45,
  "reservationRate": 45.0,
  "checkedInCount": 20,
  "availableSeats": 55
}
```

---

### 예약 목록 조회
`GET /api/host/events/{eventId}/reservations`

**Query Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| scheduleId | Long | N | 특정 스케줄 필터 |
| searchKeyword | String | N | 이름/전화번호 검색 |
| page | Integer | N | 페이지 번호 (0부터) |
| size | Integer | N | 페이지 크기 |

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 123,
      "guestName": "홍길동",
      "ticketCount": 2,
      "status": "CONFIRMED",
      "isCheckedIn": false,
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "totalElements": 45,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

---

### 예약 상세 조회
`GET /api/host/reservations/{reservationId}`

**Response (200 OK)**
```json
{
  "id": 123,
  "qrToken": "...",
  "guestName": "홍길동",
  "guestPhoneNumber": "01012345678",
  "ticketCount": 2,
  "status": "CONFIRMED",
  "isCheckedIn": false,
  "eventTitle": "2024 신년 콘서트",
  "eventLocation": "서울시 강남구",
  "schedule": {...},
  "answers": [...],
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### 예약 취소 (호스트)
`DELETE /api/host/reservations/{reservationId}`

**Response (204 No Content)**

---

### QR 체크인
`POST /api/host/checkin`

**Request**
```json
{
  "qrToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (200 OK)**
```json
{
  "message": "입장 완료",
  "guestName": "홍길동",
  "ticketCount": 2
}
```

---

### 수동 체크인
`POST /api/host/reservations/{reservationId}/checkin`

**Response (200 OK)**
```json
{
  "message": "입장 완료",
  "guestName": "홍길동",
  "ticketCount": 2
}
```

---

### 스케줄별 현황 조회
`GET /api/host/events/{eventId}/schedules/status`

**Response (200 OK)**
```json
[
  {
    "scheduleId": 1,
    "startTime": "2024-01-20T14:00:00",
    "endTime": "2024-01-20T16:00:00",
    "maxCapacity": 100,
    "currentCount": 45,
    "reservations": [
      {
        "id": 123,
        "guestName": "홍길동",
        "ticketCount": 2,
        "isCheckedIn": false
      }
    ]
  }
]
```

---

### 이미지 업로드 Presigned URL 발급
`POST /api/host/images/presigned-url`

**Request**
```json
{
  "fileName": "event-banner.jpg",
  "contentType": "image/jpeg"
}
```

**Response (200 OK)**
```json
{
  "presignedUrl": "https://s3.amazonaws.com/...",
  "imageUrl": "https://s3.amazonaws.com/bucket/images/uuid.jpg"
}
```

---

## 에러 응답

모든 에러는 다음 형식으로 반환됩니다:

```json
{
  "status": 400,
  "message": "에러 메시지"
}
```

| Status | Description |
|--------|-------------|
| 400 | Bad Request - 잘못된 요청 |
| 401 | Unauthorized - 인증 필요 |
| 403 | Forbidden - 권한 없음 |
| 404 | Not Found - 리소스 없음 |
| 409 | Conflict - 중복 예약 등 충돌 |
| 500 | Internal Server Error - 서버 오류 |
