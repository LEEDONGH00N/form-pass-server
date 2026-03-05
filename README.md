# Form-PASS Server

<img width="1228" height="576" alt="스크린샷 2026-03-05 오후 6 34 35" src="https://github.com/user-attachments/assets/d58f4bbb-e301-4c05-99b5-5465f444a3ea" />

호스트를 위한 간편 예약 관리 솔루션의 백엔드 서버입니다.

> 이벤트 생성 → 예약 폼 공유 → 선착순 예약 → QR 체크인까지 하나의 플로우로 처리합니다.

**서비스 링크** : https://www.form-pass.life

**프론트엔드** : https://github.com/LEEDONGH00N/form-pass-client

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.12 |
| ORM | Spring Data JPA (Hibernate) |
| Database | MySQL 8.0 (RDS), H2 (테스트) |
| Auth | Spring Security + JWT (jjwt 0.11.5) |
| Infra | AWS EC2, RDS, S3 |
| CI/CD | GitHub Actions |
| Monitoring | Spring Actuator + Micrometer Prometheus |
| API Docs | Springdoc OpenAPI 2.7.0 (Swagger UI) |
| Mail | Spring Mail (Google SMTP) |
| Cache | Caffeine (이메일 인증코드 TTL 관리) |
| Build | Gradle |

---

## 인프라 구조

```
[Client - Vercel]
        │
        ▼ HTTPS
[EC2 Instance]
  ├── Nginx (Reverse Proxy, SSL)
  └── Spring Boot (ticket-form.jar)
        ├── RDS MySQL (데이터 저장)
        ├── S3 (이미지 저장)
        └── Google SMTP (메일 발송)
```

---

## ERD

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

**Enum 타입:**
- `Role` : HOST
- `ReservationStatus` : CONFIRMED, CANCELLED
- `QuestionType` : TEXT, CHECKBOX, RADIO

---

## API 명세

### Auth (`/api/auth`) - Public

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/signup` | 호스트 회원가입 |
| POST | `/api/auth/login` | 로그인 (JWT 발급) |
| POST | `/api/auth/email/send` | 이메일 인증코드 발송 |
| POST | `/api/auth/email/verify` | 인증코드 확인 |

### Event - Guest (`/api/events`) - Public

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/events` | 공개 이벤트 목록 조회 |
| GET | `/api/events/{eventCode}` | 이벤트 상세 조회 (이벤트 코드) |

### Event - Host (`/api/host/events`) - ROLE_HOST

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/host/events` | 이벤트 생성 |
| GET | `/api/host/events` | 내 이벤트 목록 조회 |
| GET | `/api/host/events/{eventId}` | 내 이벤트 상세 조회 |
| PUT | `/api/host/events/{eventId}` | 이벤트 수정 |
| PATCH | `/api/host/events/{eventId}/visibility` | 공개/비공개 전환 |

### Reservation - Guest (`/api/reservations`) - Public

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/reservations` | 예약 생성 |
| GET | `/api/reservations/{id}` | 예약 상세 조회 |
| GET | `/api/reservations/qr/{qrToken}` | QR 토큰으로 예약 조회 |
| DELETE | `/api/reservations/{id}` | 예약 취소 |
| POST | `/api/reservations/lookup` | 이름+전화번호로 내 예약 조회 |

### Reservation - Host (`/api/host`) - ROLE_HOST

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/host/events/{eventId}/dashboard` | 대시보드 통계 |
| GET | `/api/host/events/{eventId}/reservations` | 예약 목록 (페이징, 검색) |
| GET | `/api/host/reservations/{reservationId}` | 예약 상세 |
| PATCH | `/api/host/reservations/{reservationId}/cancel` | 예약 취소 |
| POST | `/api/host/checkin` | QR 체크인 |
| PATCH | `/api/host/reservations/{reservationId}/checkin` | 수동 체크인 |
| GET | `/api/host/events/{eventId}/schedules-status` | 스케줄별 현황 |

### Image (`/api/host/s3`) - ROLE_HOST

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/host/s3/presigned-url` | S3 Presigned URL 발급 |

### Monitoring - Public

| Endpoint | 설명 |
|----------|------|
| `/health` | 서버 상태 확인 |
| `/actuator/prometheus` | Prometheus 메트릭 |
| `/swagger-ui/index.html` | API 문서 (Swagger UI) |

---

## 핵심 기능 상세

### 동시성 제어 (선착순 예약)

```
요청 A ──▶ ┌─────────────────────────────────┐
           │ EventSchedule (scheduleId = 1)  │
           │ PESSIMISTIC_WRITE Lock 획득     │
           │ reservedCount 검증 → 증가       │
           │ 예약 생성 → Lock 해제           │
요청 B ──▶ └─────────────────────────────────┘ ──▶ Lock 대기 후 순차 처리
```

- `EventScheduleRepository`에서 `@Lock(PESSIMISTIC_WRITE)`로 row-level 락 적용
- `reservedCount >= maxCapacity` 시 `IllegalStateException` (409)
- 동일 스케줄 + 전화번호 중복 예약 거부

### QR 체크인 플로우

```
예약 확정 → UUID 기반 qrToken 발급
         → 게스트가 QR 코드 제시
         → 호스트가 QR 스캔 (POST /api/host/checkin)
         → 또는 수동 체크인 (PATCH /api/host/reservations/{id}/checkin)
         → isCheckedIn = true 업데이트
```

### 이미지 업로드 (Presigned URL)

```
1. 클라이언트 → POST /api/host/s3/presigned-url (fileName, contentType)
2. 서버 → S3 Presigned PUT URL + 최종 fileUrl 반환
3. 클라이언트 → S3에 직접 PUT 업로드
4. 클라이언트 → 이벤트 생성 시 fileUrl을 images 목록에 포함
```

---

## 프로젝트 구조

```
src/main/java/com/example/reservation_solution/
├── api/
│   ├── auth/                          # 인증/회원
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── domain/                    # Host, Role
│   │   └── dto/
│   ├── event/                         # 이벤트 관리
│   │   ├── controller/                # EventController(호스트), GuestEventController
│   │   ├── service/
│   │   ├── repository/
│   │   ├── domain/                    # Event, EventSchedule, FormQuestion, EventImage
│   │   └── dto/
│   ├── reservation/                   # 예약
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── domain/                    # Reservation, FormAnswer, ReservationStatus
│   │   └── dto/
│   └── host/                          # 호스트 전용 (대시보드, 체크인)
│       ├── controller/
│       ├── service/
│       └── dto/
└── global/
    ├── config/                        # SecurityConfig, SwaggerConfig, AwsConfig
    ├── security/                      # JwtProvider, JwtAuthenticationFilter
    ├── exception/                     # BusinessException, GlobalExceptionHandler
    ├── mail/                          # GoogleMailService (비동기)
    ├── image/                         # S3Service, S3Controller
    ├── common/domain/                 # BaseTimeEntity (JPA Auditing)
    ├── docs/                          # Swagger @Operation 인터페이스
    └── util/                          # EncryptionUtils, CodeGenerator
```

---

## 실행 방법

### 사전 조건

- Java 17+
- MySQL 8.0 (local 프로필) 또는 H2 (test 프로필)
- Gradle 8+

### 로컬 실행

```bash
# 1. 프로젝트 클론
git clone https://github.com/LEEDONGH00N/form-pass-server.git
cd form-pass-server

# 2. local 프로필로 실행 (localhost MySQL 필요)
./gradlew bootRun -Dspring.profiles.active=local

# 3. Swagger UI 확인
open http://localhost:8080/swagger-ui/index.html
```

### 테스트 실행

```bash
# 전체 테스트 (H2 인메모리 DB 사용)
./gradlew clean test

# 특정 테스트 클래스 실행
./gradlew test --tests "ReservationSolutionApplicationTests"
```

### 빌드

```bash
# JAR 빌드 (테스트 제외)
./gradlew clean build -x test

# 결과물: build/libs/ticket-form.jar
```

---

## 환경 설정

### 프로필

| 프로필 | DB | DDL | 용도 |
|--------|-----|-----|------|
| `dev` (기본) | AWS RDS MySQL | create | 운영 |
| `local` | localhost MySQL (3306) | create | 로컬 개발 |
| `test` | H2 In-Memory (MODE=MySQL) | create-drop | 테스트 |

### 주요 설정값

```yaml
# JWT
jwt:
  secret: {BASE64_ENCODED_SECRET}     # 32자 이상
  expiration: 1800000                  # 30분

# 암호화
encryption:
  secret-key: {AES_256_KEY}           # 전화번호 암호화용

# AWS S3
cloud:
  aws:
    s3:
      bucket: form-pass-images
    region:
      static: ap-northeast-2
```

---

## CI/CD

### CI (Pull Request → main)

```
PR 생성 → GitHub Actions → ./gradlew clean test → 테스트 결과 PR 코멘트
```

### CD (Push → main)

```
main 병합 → GitHub Actions → JAR 빌드 → SCP로 EC2 전송 → deploy 스크립트 실행
```

**필요 Secrets:**
- `EC2_HOST`, `EC2_USERNAME`, `EC2_KEY` : EC2 접속 정보
- `APPLICATION_DEV_YML` : 운영 환경 설정 파일

---

## 모니터링

| 엔드포인트 | 용도 |
|-----------|------|
| `/actuator/health` | 서버 상태 (DB, Disk 포함) |
| `/actuator/prometheus` | Prometheus 메트릭 수집 |

**수집 메트릭:** HTTP 요청 응답시간 분포 (P95, P99), JVM 메모리/GC, Tomcat 스레드 풀

---

## 부하 테스트

k6를 사용하여 예약 생성 API의 동시성 안정성을 검증합니다.

```bash
# Spike 테스트 (50 → 100 VU)
k6 run k6/reservation-test.js

# Constant 테스트 (50 VU 고정)
k6 run k6/reservation-constant-test.js
```

### 테스트 결과 요약

| 시나리오 | VU | 총 요청 | 평균 응답 | p95 | 에러율 | 초과 예약 |
|----------|-----|--------|----------|-----|--------|----------|
| Spike (100 VU 피크) | 0→100 | 11,200건 | 706ms | 1,000ms | 0% | 0건 |
| Constant (50 VU) | 50 | 8,152건 | 236ms | 567ms | 0% | 0건 |

상세 결과: [`docs/load-test-report.md`](docs/load-test-report.md), [`docs/constant-test-report.md`](docs/constant-test-report.md)
