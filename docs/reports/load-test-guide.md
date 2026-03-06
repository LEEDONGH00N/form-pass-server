# 부하 테스트 가이드

k6를 활용한 부하 테스트 작성 및 실행 가이드입니다.

---

## 디렉토리 구조

```
k6/
  env/
    production.env                   ← 운영 환경 변수
    local.env                        ← 로컬 환경 변수
  sql/
    reset-reservations.sql           ← 예약 데이터 초기화 SQL
  common.js                         ← 범용 GET 엔드포인트 부하 테스터
  reservation-helpers.js             ← 예약 테스트 공통 모듈 (메트릭, 헬퍼)
  reservation-test.js                ← 예약 Ramp-up/Spike 테스트
  reservation-constant-test.js       ← 예약 Constant VU 테스트

perf.sh                              ← 부하 테스트 실행 래퍼

docs/reports/                        ← 테스트 결과 리포트
  load-test-report.md
  constant-test-report.md
```

---

## 환경 설정

환경별 설정은 `k6/env/` 디렉토리의 `.env` 파일로 관리합니다.

### 환경 파일

| 파일 | BASE_URL | 용도 |
|------|----------|------|
| `production.env` | https://api.form-pass.life | 운영 서버 대상 테스트 |
| `local.env` | http://localhost:8080 | 로컬 개발 서버 테스트 |

### 환경 파일 형식

```bash
BASE_URL=https://api.form-pass.life
SCHEDULE_ID=1
DENYLIST=/api/auth/email/send,/api/auth/email/verify,/api/host/s3/presigned-url
```

### 규칙
- **시크릿(TOKEN 등)은 env 파일에 넣지 않습니다.** 실행 시 `--token` 옵션으로 전달하십시오.
- 새 환경이 필요하면 `k6/env/{환경명}.env` 파일을 추가하십시오.
- CLI 옵션(`--base-url`, `--schedule-id` 등)은 env 파일보다 우선합니다.

---

## 실행 방법

Docker 기반으로 k6를 실행합니다. `perf.sh` 래퍼를 사용하며, `--env`는 필수입니다.

### 기본 사용법

```bash
# 예약 Spike 테스트 (운영, warm-up + 대시보드)
./perf.sh --env production --script reservation-test.js --warmup --dashboard

# 예약 Constant 테스트 (운영)
./perf.sh --env production --script reservation-constant-test.js --warmup --dashboard

# 로컬 서버 대상 테스트
./perf.sh --env local --script reservation-test.js --dashboard

# 특정 GET 엔드포인트 부하 테스트
./perf.sh --env production --endpoints "/api/events,/health" --dashboard

# 인증 필요 엔드포인트
./perf.sh --env production --endpoints "/api/host/events" --token "$TOKEN" --dashboard

# 특정 스케줄 ID 지정 (env 파일 값 오버라이드)
./perf.sh --env production --script reservation-test.js --schedule-id 3 --dashboard
```

### perf.sh 전체 옵션

| 옵션 | 설명 | 필수 | 기본값 |
|------|------|------|--------|
| `--env <name>` | 환경 파일 (production, local) | **필수** | - |
| `--script <file>` | k6 스크립트 파일명 | X | common.js |
| `--base-url <url>` | API 서버 URL (env보다 우선) | X | env 파일 값 |
| `--endpoints <csv>` | 테스트할 GET 엔드포인트 (common.js 전용) | X | /health |
| `--denylist <csv>` | 차단할 엔드포인트 (env보다 우선) | X | env 파일 값 |
| `--token <token>` | Bearer 토큰 | X | - |
| `--test-id <id>` | 테스트 ID | X | exp-YYYYMMDD-HHMM |
| `--schedule-id <id>` | 테스트 대상 스케줄 ID (env보다 우선) | X | env 파일 값 |
| `--warmup` | warm-up 후 main 실행 (2회) | X | false |
| `--dashboard` | 웹 대시보드 (http://localhost:5665) | X | false |

### 대시보드 확인

`--dashboard` 옵션 사용 시 브라우저에서 확인:

```
http://localhost:5665
```

---

## 테스트 전 준비

### DB 데이터 초기화

예약 테스트 실행 전 반드시 기존 데이터를 초기화하십시오:

```bash
# 로컬 DB
mysql -u root -p reservation-local < k6/sql/reset-reservations.sql

# 운영 DB (RDS 접속 후)
mysql -h {RDS_HOST} -u {USER} -p {DATABASE} < k6/sql/reset-reservations.sql
```

초기화 SQL 위치: `k6/sql/reset-reservations.sql`

### 차단 목록 (Denylist)

아래 엔드포인트는 외부 부작용이 있으므로 부하 테스트에서 차단합니다:

| 엔드포인트 | 이유 |
|-----------|------|
| `/api/auth/email/send` | 실제 이메일 발송 |
| `/api/auth/email/verify` | 인증 상태 변경 |
| `/api/host/s3/presigned-url` | S3 URL 생성 비용 |

---

## 스크립트 작성 규칙

### 파일 템플릿

모든 k6 스크립트는 아래 구조를 따르십시오:

```javascript
/**
 * ===========================================
 * Form PASS {테스트 대상} 부하 테스트 ({시나리오 유형})
 * ===========================================
 *
 * [테스트 시나리오]
 * {시나리오 설명}
 *
 * [테스트 시간] {총 소요 시간}
 *
 * [성공 기준]
 * - p(95) 응답 시간 < {기준}ms
 * - 에러율 < {기준}%
 *
 * [실행 방법]
 * $ ./perf.sh --env {환경} --script {파일명} --warmup --dashboard
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

// ============================================
// Test Configuration
// ============================================
export const options = {
  // stages 또는 vus + duration
  thresholds: { ... },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],
};

// ============================================
// Main
// ============================================
export default function () { ... }
export function setup() { ... }
export function teardown() { ... }
```

### 환경변수

| 변수 | 설명 | 제공 방식 |
|------|------|----------|
| `BASE_URL` | API 서버 URL | env 파일 |
| `SCHEDULE_ID` | 테스트 대상 스케줄 ID | env 파일 |
| `DENYLIST` | 차단 엔드포인트 | env 파일 |
| `TEST_ID` | 테스트 식별자 | perf.sh 자동 생성 |
| `TOKEN` | Bearer 토큰 | CLI 옵션 (--token) |
| `WARMUP` | warm-up 모드 여부 | perf.sh 관리 |

### 공통 헤더

모든 요청에 아래 헤더를 포함하십시오:

```javascript
headers: {
  'X-Test-Id': TEST_ID,      // 서버 로그 추적용
  'X-Perf-Test': 'true',     // 부하 테스트 식별
}
```

### 공통 모듈 패턴

도메인별 테스트는 공통 모듈을 먼저 만들고, 테스트 스크립트에서 import합니다:

```
{도메인}-helpers.js     ← 메트릭, 헬퍼 함수, 요청 로직
{도메인}-test.js        ← Spike/Ramp-up 시나리오
{도메인}-constant-test.js  ← Constant VU 시나리오
```

```javascript
// reservation-test.js
import { createReservation } from './reservation-helpers.js';

export default function () {
  createReservation();
}
```

---

## 결과 리포트 작성

테스트 완료 후 결과를 `docs/reports/` 디렉토리에 리포트로 작성하십시오.

### 파일명 규칙

`{테스트대상}-{시나리오}-report.md`

예: `load-test-report.md`, `constant-test-report.md`

### 리포트 필수 항목

1. **테스트 환경** — 서버, DB, 클라이언트, 사용한 env 파일
2. **테스트 시나리오** — 부하 패턴, VU, 시간
3. **성공 기준** — Thresholds
4. **응답 시간** — avg, med, p90, p95, p99, max
5. **처리량** — 총 요청 수, RPS, 성공률
6. **병목 분석 및 결론**
7. **실행 명령어** — 재현 가능하도록 전체 명령어 기재

---

## 시나리오 유형

| 유형 | 목적 | VU 패턴 | 사용 시점 |
|------|------|---------|----------|
| **Constant** | 기준 성능(Baseline) 측정 | 고정 VU | 기능 구현 후 초기 검증 |
| **Ramp-up/Spike** | 부하 증가 시 내구성 확인 | 점진적 증가 → 피크 | 동시성 로직 검증 |
| **Stress** | 한계점 탐색 | 지속적 증가 | 최대 수용량 파악 |
| **Soak** | 장시간 안정성 | 고정 VU, 장시간 | 메모리 누수 등 확인 |

---

## 전체 워크플로우 요약

```
1. 환경 확인       → k6/env/{환경}.env 내용 확인
2. DB 초기화       → k6/sql/reset-reservations.sql 실행
3. 테스트 실행     → ./perf.sh --env {환경} --script {스크립트} --warmup --dashboard
4. 대시보드 확인   → http://localhost:5665
5. 결과 리포트     → docs/reports/{테스트명}-report.md 작성
```
