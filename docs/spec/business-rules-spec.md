# 비즈니스 규칙

---

## 1. 회원가입 / 인증

### 이메일 인증
- 6자리 인증코드를 이메일로 발송
- 인증코드 유효시간: **5분** (Caffeine Cache TTL)
- 인증 완료 후 회원가입 허용시간: **1분**
- 최대 동시 저장 인증코드: 1,000건

### 회원가입 조건
- 이메일 중복 불가 (`existsByEmail` 검증)
- 이메일 인증 완료 필수 (`isEmailVerified` 검증)
- 비밀번호는 BCrypt로 해시 저장
- 가입 시 역할은 `ROLE_HOST`로 고정

### 로그인
- 이메일 + 비밀번호 검증
- 성공 시 JWT 토큰 발급 (30분 만료)
- JWT Claims: subject(email), role(HOST)

---

## 2. 이벤트 관리

### 이벤트 생성
- 호스트만 생성 가능 (ROLE_HOST)
- 10자리 고유 `eventCode` 자동 생성 (CodeGenerator)
- 중복 eventCode 발생 시 재생성
- 최소 1개 이상의 스케줄 필수 (`@NotEmpty`)
- 질문(FormQuestion)은 선택사항
- 이미지는 S3 URL 목록으로 전달

### 이벤트 수정
- **소유권 검증**: `event.getHost().getEmail().equals(hostEmail)` 필수
- 스케줄, 질문, 이미지는 전체 교체 (clear → re-add)
- **질문 수정 제한**: 해당 이벤트에 이미 폼 응답(FormAnswer)이 존재하면 질문 수정 불가
  - `formAnswerRepository.existsByEventId(eventId)` 검증

### 이벤트 조회
- 공개 이벤트(`isPublic = true`): 누구나 조회 가능
- 비공개 이벤트: 호스트 본인만 조회 가능
  - 비소유자 접근 시 `AccessDeniedException` (403)

### 공개/비공개 전환
- 소유권 검증 후 `isPublic` 값 변경

---

## 3. 예약

### 예약 생성 플로우

```
1. scheduleId로 EventSchedule 조회 (PESSIMISTIC_WRITE Lock)
2. 전화번호 AES-256 암호화
3. 중복 예약 검증 (같은 스케줄 + 같은 전화번호 + CONFIRMED 상태)
4. 잔여 좌석 검증 (reservedCount + ticketCount <= maxCapacity)
5. reservedCount 증가
6. UUID 기반 qrToken 발급
7. 폼 응답 처리 (FormAnswer 생성)
8. 필수 질문 응답 여부 검증
9. 예약 저장 (status = CONFIRMED)
```

### 동시성 제어

```
요청 A ──▶ ┌──────────────────────────────┐
           │ PESSIMISTIC_WRITE Lock 획득  │
           │ reservedCount 검증 → 증가    │
           │ 예약 생성 → Lock 해제        │
요청 B ──▶ └──────────────────────────────┘ ──▶ Lock 대기 → 순차 처리
```

- `EventScheduleRepository.findByIdWithLock()` → `@Lock(PESSIMISTIC_WRITE)`
- Row-level Lock으로 같은 스케줄에 대한 동시 예약을 순차 처리
- `reservedCount >= maxCapacity` 시 `IllegalStateException` (409)

### 중복 예약 방지
- 동일 스케줄 + 동일 전화번호(암호화된 값) + CONFIRMED 상태 조합으로 검증
- 중복 시 `IllegalStateException` (409)

### 폼 응답 규칙
- `questionId`로 FormQuestion 조회 후 FormAnswer 생성
- 필수 질문(`isRequired = true`)에 대해 응답 여부 검증
- 단, 빌트인 질문("이름", "전화번호", "name", "phone")은 검증에서 제외

### 예약 취소 조건
- 이미 **취소된** 예약: 취소 불가 (409)
- 이미 **체크인된** 예약: 취소 불가 (409)
- 취소 시 `reservedCount` 감소 (ticketCount만큼)
- 상태를 `CANCELLED`로 변경

### 내 예약 조회 (lookup)
- 이름 + 전화번호로 조회
- 전화번호를 암호화하여 DB 검색
- CONFIRMED 상태인 예약만 반환
- 생성일 역순 정렬

---

## 4. 체크인

### QR 체크인
1. `qrToken`으로 예약 조회
2. 소유권 검증 (예약의 이벤트 호스트 == 요청자)
3. 체크인 가능 여부 검증
4. `isCheckedIn = true` 업데이트

### 수동 체크인
1. `reservationId`로 예약 조회
2. 소유권 검증
3. 체크인 가능 여부 검증
4. `isCheckedIn = true` 업데이트

### 체크인 불가 조건
- 이미 체크인 완료된 예약 (409)
- 취소된 예약 (409)
- 본인 소유 이벤트가 아닌 경우 (403)

---

## 5. 호스트 대시보드

### 통계 계산
- `totalSeats`: 이벤트 전체 스케줄의 maxCapacity 합산
- `reservedCount`: 전체 스케줄의 reservedCount 합산
- `reservationRate`: (reservedCount / totalSeats) * 100
- `checkedInCount`: 이벤트의 모든 CONFIRMED 예약 중 isCheckedIn = true 수
- `availableSeats`: totalSeats - reservedCount

### 예약 목록 조회
- 페이징 지원 (기본: 20건, createdAt DESC)
- 스케줄 ID로 필터링 가능
- 이름 또는 전화번호로 검색 가능 (LIKE 쿼리)
- 전화번호는 암호화된 값을 복호화하여 응답

### 스케줄별 현황
- 이벤트의 모든 스케줄 목록
- 각 스케줄의 capacity, 현재 예약 수
- 각 스케줄의 CONFIRMED 예약 목록 포함

---

## 6. 이미지 업로드

### Presigned URL 플로우
1. 호스트가 `fileName` + `contentType` 전달
2. 서버가 S3 Presigned PUT URL 생성 (유효시간 제한)
3. 클라이언트가 Presigned URL로 S3에 직접 업로드
4. 이벤트 생성/수정 시 S3 URL을 이미지 목록에 포함

---

## 7. 소유권 검증 (공통)

모든 호스트 전용 API에서 공통으로 적용:

```java
event.getHost().getEmail().equals(hostEmail)
```

- 불일치 시 `AccessDeniedException` (403) 또는 `ForbiddenException` (403)
- 검증 대상: 이벤트 CRUD, 예약 조회/취소, 체크인, 대시보드

---

## 8. 개인정보 보호

### 전화번호 암호화
- 저장 시: 평문 → AES-256 암호화 → DB 저장
- 조회 시: DB → AES-256 복호화 → 응답
- `EncryptionUtils` 클래스에서 처리
- 암호화 키: `application.yml`의 `encryption.secret-key`

### 비밀번호
- BCrypt 해시 저장
- 로그인 시 `PasswordEncoder.matches()`로 검증
