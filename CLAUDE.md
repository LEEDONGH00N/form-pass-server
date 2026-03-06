# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Form PASS is an event reservation system built with Spring Boot 3.4.12 and Java 17. Hosts create events with time-slot schedules and custom form questions; guests reserve tickets and check in via QR codes. Base package: `com.example.reservation_solution`.

## Build Commands

```bash
./gradlew clean test                          # Run all tests
./gradlew clean build -x test                 # Build without tests
./gradlew bootRun                             # Run locally (default profile: dev)
./gradlew test --tests "TestClassName"        # Run a specific test
./gradlew bootJar                             # Build production JAR (ticket-form.jar)
```

## Architecture

**Domain-based package structure under `api/`:**
```
api/
  auth/          → Host signup, login, JWT tokens, email verification
  event/         → Event CRUD, schedules, form questions (guest + host controllers)
  reservation/   → Guest reservation creation, lookup, cancellation
  host/          → Host-only: dashboard stats, reservation list, QR/manual check-in
global/
  security/      → JwtProvider, JwtAuthenticationFilter, HostUserDetailsService
  exception/     → BusinessException hierarchy + GlobalExceptionHandler
  config/        → SecurityConfig, SwaggerConfig, AwsConfig
  mail/          → Async email sending (Google SMTP)
  image/         → S3 presigned URL generation
  common/domain/ → BaseTimeEntity (createdAt/updatedAt auditing)
  docs/          → Swagger @Operation annotation interfaces per endpoint
  util/          → EncryptionUtils (AES for phone numbers), CodeGenerator
```

Each domain package follows: `controller/ → service/ → repository/ → domain/ → dto/`

**Key Domain Relationships:**
- `Host` → owns many `Event` (via eventId)
- `Event` → has `EventSchedule[]`, `FormQuestion[]`, `EventImage[]`
- `EventSchedule` → has `Reservation[]` (capacity-controlled with pessimistic locking)
- `Reservation` → has `FormAnswer[]`, a `qrToken` (UUID) for check-in, and `ReservationStatus` (CONFIRMED/CANCELLED)

## Conventions

- **Lombok** is used throughout (entities, DTOs). Use `@Getter`, `@NoArgsConstructor(access = PROTECTED)` on entities; `@Builder` on DTOs
- **Error messages are in Korean** (e.g., `"존재하지 않는 이벤트입니다"`)
- **Exception pattern**: Services throw `IllegalArgumentException` (400), `IllegalStateException` (409), `AccessDeniedException` (403), or `BusinessException` subclasses (`BadRequestException`, `ConflictException`, `ForbiddenException`, `NotFoundException`, `UnauthorizedException`). All handled by `GlobalExceptionHandler`
- **Host ownership validation**: Services verify `event.getHost().getEmail().equals(hostEmail)` before allowing mutations
- **Transactions**: Services use `@Transactional(readOnly = true)` at class level, `@Transactional` on write methods
- **Phone number encryption**: Guest phone numbers are encrypted via `EncryptionUtils` (AES-256) before storage and decrypted on read
- **Swagger docs**: Each controller method's `@Operation` annotation is defined in a separate interface in `global/docs/` (e.g., `EventControllerDocs`), which the controller implements
- **Entity creation**: Entities use static factory methods (`create(...)`) instead of public constructors

## Concurrency Control

- `EventSchedule.reservedCount` is protected by `@Lock(LockModeType.PESSIMISTIC_WRITE)` in `EventScheduleRepository` to prevent overbooking race conditions
- Duplicate reservation check: same phone number + same schedule combination is rejected

## Security

- JWT Bearer Token auth (Authorization header), 30-minute expiry, HS256 signing (jjwt 0.11.5)
- `JwtAuthenticationFilter` skips `/api/auth/**` paths
- Public: `/api/auth/**`, `/api/events/**`, `/api/reservations/**`, `/actuator/**`, Swagger UI
- Protected: `/api/host/**` requires `ROLE_HOST`
- Authenticated user's email is extracted from `SecurityContextHolder` principal in controllers
- CORS allowed origins: `localhost:3000`, `localhost:3001`, `form-pass.life`, `form-pass-client.vercel.app`

## Testing

- Test profile (`application-test.yml`): H2 in-memory DB with `MODE=MySQL`, mock mail config, dedicated JWT secret
- Tests run with: `./gradlew clean test`
- Currently only a context-load test exists (`ReservationSolutionApplicationTests`)

## Profiles

- `dev` (default, set in `application.yml`): Production RDS MySQL, Prometheus monitoring enabled
- `local`: localhost MySQL on port 3306, `ddl-auto: create`, debug logging
- `test`: H2 in-memory, used automatically in test runs

## CI/CD

- **CI**: PRs to `main` → `./gradlew clean test` + publish test results as PR comments
- **CD**: Push to `main` → build JAR (skip tests) → SCP to EC2 → run deploy script
- `application-dev.yml` is injected from GitHub Secrets (`APPLICATION_DEV_YML`) during CD

## Key Dependencies

- **JWT**: jjwt 0.11.5 | **AWS SDK**: v2.27.21 (S3) | **API Docs**: Springdoc OpenAPI 2.7.0
- **Monitoring**: Spring Actuator + Micrometer Prometheus | **Cache**: Caffeine

## 필수 참조 문서

코드 작성 전 반드시 아래 문서를 읽고 규칙을 따르십시오.

| 문서 | 역할 | 필수 시점 |
|------|------|----------|
| `rule.md` | AI 그라운드 룰 (개발 프로세스, 아키텍처, 코드 스타일, 테스트 규칙) | **모든 작업 전** |
| `spec.md` | 기능 명세 인덱스 → `docs/spec/` 하위 문서 | 기능 구현·수정 시 |

### 프로젝트 문서 구조

```
rule.md              ← AI 코딩 규칙 (개발 프로세스 0번 ~ Git 규칙 13번)
spec.md              ← 기능 명세 인덱스
docs/
  spec/              ← 기능 명세 (*-spec.md)
    api-spec.md
    erd-spec.md
    business-rules-spec.md
    domain-spec.md
  reports/           ← 부하 테스트 결과 리포트
k6/                  ← k6 부하 테스트 스크립트
perf.sh              ← 부하 테스트 실행 래퍼
```
