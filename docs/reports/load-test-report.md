# Form PASS 부하 테스트 결과 보고서

**테스트 일시:** 2026-02-18
**테스트 도구:** k6
**테스트 대상:** 예약 생성 API (`POST /api/reservations`)

---

## 1. 테스트 환경

| 항목 | 내용 |
|------|------|
| 서버 | AWS EC2 (운영 환경) |
| API 엔드포인트 | https://api.form-pass.life |
| 데이터베이스 | MySQL (RDS) |
| 테스트 클라이언트 | 로컬 머신 |

---

## 2. 테스트 시나리오

### 2.1 부하 패턴

```
VU(Virtual Users)
     │
 100 ┤                    ┌────────────┐
     │                   ╱              ╲
  50 ┤      ┌───────────┘                ╲
     │     ╱                               ╲
   0 ┼────┴─────────────────────────────────┴────▶ Time
     0    30s    1m30s    2m    3m    3m30s   4m
        Ramp   Sustain  Spike  Peak  Ramp
         up              up           down
```

| 단계 | 시간 | VU 수 | 목적 |
|------|------|-------|------|
| Ramp-up | 0 ~ 30초 | 0 → 50 | 점진적 부하 증가 |
| Sustain | 30초 ~ 1분 30초 | 50 유지 | 안정성 확인 |
| Spike | 1분 30초 ~ 2분 | 50 → 100 | 급격한 부하 증가 대응 |
| Peak | 2분 ~ 3분 | 100 유지 | 최대 부하 내구성 |
| Ramp-down | 3분 ~ 3분 30초 | 100 → 0 | 정상 복구 확인 |

### 2.2 테스트 요청

```json
POST /api/reservations
{
  "scheduleId": 1,
  "guestName": "tester_1_0",
  "guestPhoneNumber": "01012345678",
  "ticketCount": 1,
  "answers": []
}
```

### 2.3 성공 기준 (Thresholds)

| 지표 | 기준 |
|------|------|
| p(95) 응답 시간 | < 2,000ms |
| 에러율 | < 10% |

---

## 3. 테스트 결과

### 3.1 응답 시간 (http_req_duration)

| 지표 | 값 | 평가 |
|------|-----|------|
| **평균 (avg)** | 706ms | ⚠️ 높음 |
| **중앙값 (med)** | 248ms | ✅ 양호 |
| **최소 (min)** | 24ms | ✅ 양호 |
| **최대 (max)** | 4,000ms | ❌ 스파이크 |
| **p(90)** | 1,000ms | ⚠️ 경계 |
| **p(95)** | 1,000ms | ⚠️ 경계 |
| **p(99)** | 2,000ms | ⚠️ 높음 |

### 3.2 상세 시간 분석

| 구간 | 평균 시간 | 비중 |
|------|----------|------|
| TCP 연결 (connecting) | 104µs | 0.01% |
| TLS 핸드셰이크 | 163µs | 0.02% |
| 요청 전송 (sending) | 28µs | < 0.01% |
| **서버 처리 (waiting)** | **706ms** | **99.9%** |
| 응답 수신 (receiving) | 75µs | 0.01% |

> 💡 대부분의 시간이 **서버 처리(waiting)**에 소요됨

### 3.3 처리량

| 지표 | 값 |
|------|-----|
| **총 요청 수** | 11,200건 |
| **평균 RPS** | 53.41 req/s |
| **성공 건수** | 11,200건 |
| **성공률** | 100% ✅ |

### 3.4 네트워크

| 지표 | 값 |
|------|-----|
| 수신 (data_received) | 9.74 MB (46.4 kB/s) |
| 송신 (data_sent) | 3.2 MB (15.2 kB/s) |
| 요청당 응답 크기 | ~870 bytes |

### 3.5 상세 Trends

| metric | avg | max | med | min | p90 | p95 | p99 |
|--------|-----|-----|-----|-----|-----|-----|-----|
| http_req_blocked | 282µs | 99ms | 6µs | 1µs | 10µs | 12µs | 98µs |
| http_req_connecting | 104µs | 44ms | 0ms | 0ms | 0ms | 0ms | 0ms |
| **http_req_duration** | **706ms** | **4s** | **248ms** | **24ms** | **1s** | **1s** | **2s** |
| http_req_receiving | 75µs | 3ms | 65µs | 10µs | 114µs | 138µs | 211µs |
| http_req_sending | 28µs | 4ms | 25µs | 3µs | 41µs | 48µs | 78µs |
| http_req_tls_handshaking | 163µs | 27ms | 0ms | 0ms | 0ms | 0ms | 0ms |
| http_req_waiting | 706ms | 4s | 248ms | 24ms | 1s | 1s | 2s |
| iteration_duration | 1s | 4s | 749ms | 526ms | 2s | 2s | 2s |

---

## 4. VU별 성능 비교

| VU 수 | 평균 응답 시간 | p(95) | 상태 |
|-------|--------------|-------|------|
| 10 | 43ms | 78ms | ✅ 안정 |
| 50 | ~300ms | ~500ms | ⚠️ 보통 |
| 100 | 706ms | 1,000ms | ❌ 한계 도달 |

---

## 5. 병목 분석

### 5.1 현상

- **중앙값(248ms)과 평균(706ms)의 큰 차이**
  - 대부분의 요청은 빠르게 처리됨
  - 일부 요청이 매우 느려서 평균을 끌어올림

- **최대 응답 시간 4초**
  - 100 VU 피크 시점에서 일부 요청 지연 심화

### 5.2 원인 (추정)

1. **비관적 락 (Pessimistic Lock)**
   - `EventSchedule` 엔티티에 `PESSIMISTIC_WRITE` 락 사용
   - 동시 예약 시 락 획득 대기 시간 증가

2. **DB 커넥션 풀 제한**
   - HikariCP 기본 설정 (10개)으로 동시 처리 제한

3. **단일 스케줄 집중**
   - 모든 요청이 scheduleId=1로 집중
   - 실제 운영에서는 분산될 가능성 있음

---

## 6. 결론

### 6.1 현재 상태

| 항목 | 결과 |
|------|------|
| **권장 동시 사용자** | 50명 이하 |
| **최대 허용 동시 사용자** | 100명 (성능 저하 감수) |
| **평균 RPS** | 53.41 req/s |
| **총 처리량** | 11,200건 / 4분 |

### 6.2 성능 기준 충족 여부

| 기준 | 결과 |
|------|------|
| p(95) < 2,000ms | ✅ 충족 (1,000ms) |
| 에러율 < 10% | ✅ 충족 (0%) |
| 성공률 | ✅ 100% |

---

## 7. 개선 권장 사항

### 7.1 단기 개선 (인프라)

| 항목 | 현재 | 권장 |
|------|------|------|
| DB 커넥션 풀 | 10 | 20~30 |
| EC2 인스턴스 | t2.micro | t3.small 이상 |

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
```

### 7.2 중기 개선 (아키텍처)

1. **낙관적 락 검토**
   - 충돌이 적은 경우 성능 향상 가능
   - `@Version` 어노테이션 활용

2. **Redis 분산 락**
   - DB 락 대신 Redis 기반 분산 락 도입
   - Redisson 라이브러리 활용

3. **읽기/쓰기 분리**
   - 조회 API는 Read Replica 활용

### 7.3 장기 개선 (스케일링)

1. **수평 확장**
   - 로드밸런서 + 다중 인스턴스 구성

2. **메시지 큐 도입**
   - 예약 요청을 큐에 넣고 순차 처리
   - 피크 시간 부하 분산

---

## 8. 테스트 스크립트

테스트에 사용된 k6 스크립트: `k6/reservation-test.js`

```bash
# 실행 방법
k6 run --out web-dashboard=host=0.0.0.0 k6/reservation-test.js

# 대시보드 확인
http://localhost:5665
```

---

## 부록: 테스트 전 데이터 초기화

```sql
DELETE FROM form_answers WHERE reservation_id IN (
  SELECT id FROM reservations WHERE event_schedule_id = 1
);
DELETE FROM reservations WHERE event_schedule_id = 1;
UPDATE event_schedules SET reserved_count = 0, max_capacity = 100000 WHERE id = 1;
```
