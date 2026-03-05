/**
 * ===========================================
 * Form PASS 범용 GET 엔드포인트 부하 테스터
 * ===========================================
 *
 * 여러 GET 엔드포인트에 대해 동시 부하를 생성합니다.
 * DENYLIST로 외부 API 호출을 방지하고,
 * X-Test-Id 헤더로 서버 로그 추적이 가능합니다.
 *
 * 환경변수:
 *   BASE_URL   - API 서버 URL (기본: https://api.form-pass.life)
 *   ENDPOINTS  - 테스트할 엔드포인트 CSV (예: "/api/events,/health")
 *   DENYLIST   - 차단할 엔드포인트 CSV (매칭 시 즉시 fail)
 *   TEST_ID    - 테스트 식별자 (X-Test-Id 헤더에 포함)
 *   WARMUP     - "true"이면 warm-up 모드 (1 VU, 10s)
 *   TOKEN      - Bearer 토큰 (인증 필요 시)
 *
 * 실행 예시:
 *   docker run --rm \
 *     -e BASE_URL="https://api.form-pass.life" \
 *     -e ENDPOINTS="/api/events,/health" \
 *     -e DENYLIST="/api/auth/email/send" \
 *     -e TEST_ID="exp-20260227-1430" \
 *     -v "$(pwd)/k6:/scripts" \
 *     grafana/k6 run /scripts/common.js
 */

import http from 'k6/http';
import { check, fail, sleep } from 'k6';

// ============================================
// Configuration
// ============================================
const BASE_URL = __ENV.BASE_URL || 'https://api.form-pass.life';
const TEST_ID = __ENV.TEST_ID || 'unknown';
const TOKEN = __ENV.TOKEN || '';
const IS_WARMUP = (__ENV.WARMUP || 'false') === 'true';

const ENDPOINTS = (__ENV.ENDPOINTS || '/health')
  .split(',')
  .map((ep) => ep.trim())
  .filter((ep) => ep.length > 0);

const DENYLIST = (__ENV.DENYLIST || '')
  .split(',')
  .map((ep) => ep.trim())
  .filter((ep) => ep.length > 0);

// ============================================
// Options
// ============================================
export const options = IS_WARMUP
  ? {
      vus: 1,
      duration: '10s',
      thresholds: {
        http_req_duration: ['p(95)<5000'],
      },
      summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],
    }
  : {
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 30 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
      ],
      thresholds: {
        http_req_duration: ['p(95)<2000'],
      },
      summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],
    };

// ============================================
// Setup
// ============================================
export function setup() {
  console.log('========================================');
  console.log(`Mode      : ${IS_WARMUP ? 'WARM-UP (1 VU, 10s)' : 'MAIN (ramp-up stages)'}`);
  console.log(`Test ID   : ${TEST_ID}`);
  console.log(`Base URL  : ${BASE_URL}`);
  console.log(`Endpoints : ${ENDPOINTS.join(', ')}`);
  console.log(`Denylist  : ${DENYLIST.length > 0 ? DENYLIST.join(', ') : '(none)'}`);
  console.log(`Token     : ${TOKEN ? '(set)' : '(not set)'}`);
  console.log('========================================');

  // DENYLIST 검증: 요청 전에 차단 대상이 ENDPOINTS에 포함되어 있는지 확인
  for (const ep of ENDPOINTS) {
    for (const denied of DENYLIST) {
      if (ep === denied || ep.startsWith(denied)) {
        fail(`DENYLIST 위반: "${ep}" 는 차단된 엔드포인트입니다 (${denied})`);
      }
    }
  }
}

// ============================================
// Main
// ============================================
export default function () {
  const headers = {
    'X-Test-Id': TEST_ID,
    'X-Perf-Test': 'true',
  };

  if (TOKEN) {
    headers['Authorization'] = `Bearer ${TOKEN}`;
  }

  for (const ep of ENDPOINTS) {
    const url = `${BASE_URL}${ep}`;
    const params = {
      headers: headers,
      tags: { name: ep },
    };

    const response = http.get(url, params);

    check(response, {
      [`${ep} status < 400`]: (r) => r.status < 400,
    });
  }

  sleep(0.5);
}

// ============================================
// Teardown
// ============================================
export function teardown() {
  console.log('========================================');
  console.log('부하 테스트 완료');
  console.log(`Test ID: ${TEST_ID}`);
  console.log('========================================');
}
