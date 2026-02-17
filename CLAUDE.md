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

## Monitoring Stack

```bash
# Start Prometheus and Grafana
docker-compose up -d

# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
# Swagger UI: http://localhost:8080/swagger-ui.html
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

- JWT authentication with 30-minute expiration
- Public endpoints: `/api/auth/**`, `/api/events/**`, `/api/reservations/**`
- Protected endpoints: `/api/host/**` (requires HOST role)
- Guest phone numbers are encrypted in database using `EncryptionUtils`

## Testing

- Test profile uses H2 in-memory database (see `application-test.yml`)
- `ReservationConcurrencyTest` tests race conditions with pessimistic locking
- Tests run with: `./gradlew clean test`

## Key Technical Details

- **Concurrency**: `EventSchedule` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` to prevent overselling
- **Email Verification**: Uses Caffeine cache for storing verification codes
- **File Uploads**: AWS S3 presigned URLs for direct client uploads
- **Async**: `@EnableAsync` enabled for background tasks like email sending

## CI/CD

- **CI**: Pull requests to main trigger `./gradlew clean test`
- **CD**: Push to main builds JAR and deploys to EC2 via SSH
