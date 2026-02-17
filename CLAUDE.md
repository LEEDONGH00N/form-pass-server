# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Form PASS is an event reservation system built with Spring Boot 3.4.12 and Java 17. It allows hosts to manage events, schedules, and reservations with form validation and check-in functionality.

## Build Commands

```bash
# Run all tests
./gradlew clean test

# Build without tests
./gradlew clean build -x test

# Run locally (requires MySQL on localhost:3306)
./gradlew bootRun

# Run a specific test
./gradlew test --tests "TestClassName"

# Build production JAR (produces ticket-form.jar)
./gradlew bootJar
```

## Architecture

**Layered Structure:**
```
controller/     → REST API endpoints (guest/ and host/ packages)
service/        → Business logic
repository/     → JPA data access with custom queries
domain/         → JPA entities
dto/            → Request/Response objects
global/         → Cross-cutting concerns (config, security, exception, mail, image, util)
```

**Key Domain Models:**
- `Host` → Event organizer account
- `Event` → Event definition with unique eventCode
- `EventSchedule` → Time slots with capacity (uses pessimistic locking)
- `Reservation` → Booking with QR token for check-in
- `FormQuestion` / `FormAnswer` → Custom form fields per event

## Security

- JWT Bearer Token 인증 (Authorization 헤더), 30분 만료
- Public endpoints: `/api/auth/**`, `/api/events/**`, `/api/reservations/**`
- Protected endpoints: `/api/host/**` (requires HOST role)
- Guest phone numbers are encrypted in database using `EncryptionUtils`

## Testing

- Test profile uses H2 in-memory database (see `application-test.yml`)
- Tests run with: `./gradlew clean test`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Key Technical Details

- **Concurrency**: `EventSchedule` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` to prevent overselling
- **Email Verification**: Uses Caffeine cache for storing verification codes
- **File Uploads**: AWS S3 presigned URLs for direct client uploads
- **Async**: `@EnableAsync` enabled for background tasks like email sending

## CI/CD

- **CI**: Pull requests to main trigger `./gradlew clean test`
- **CD**: Push to main builds JAR and deploys to EC2 via SSH
