/**
 * ===========================================
 * Form PASS 예약 테스트 공통 모듈
 * ===========================================
 *
 * 예약 API 전용 메트릭, 헬퍼 함수, 요청 로직을 제공합니다.
 * reservation-test.js, reservation-constant-test.js에서 import하여 사용합니다.
 *
 * 환경변수:
 *   BASE_URL     - API 서버 URL (기본: https://api.form-pass.life)
 *   SCHEDULE_ID  - 테스트 대상 스케줄 ID (기본: 1)
 *   TEST_ID      - 테스트 식별자 (X-Test-Id 헤더에 포함)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ============================================
// Constants (환경변수에서 읽기)
// ============================================
export const BASE_URL = __ENV.BASE_URL || 'https://api.form-pass.life';
export const SCHEDULE_ID = parseInt(__ENV.SCHEDULE_ID || '1', 10);
export const TEST_ID = __ENV.TEST_ID || 'unknown';

// ============================================
// Custom Metrics
// ============================================
export const reservationSuccess = new Counter('reservation_success');
export const reservationFailed = new Counter('reservation_failed');
export const errorRate = new Rate('error_rate');
export const reservationDuration = new Trend('reservation_duration');

// ============================================
// Helper Functions
// ============================================

/**
 * 고유한 전화번호 생성 (11자리: 010XXXXXXXX)
 * VU ID + Iteration + Timestamp 조합으로 중복 방지
 */
export function generateUniquePhone() {
  const timestamp = Date.now() % 100000000;
  const unique = (__VU * 1000000) + (__ITER * 1000) + (timestamp % 1000);
  const num = unique % 100000000;
  return `010${String(num).padStart(8, '0')}`;
}

// ============================================
// Main Test Logic
// ============================================

/**
 * 예약 생성 요청을 보내고 결과를 검증합니다.
 * 각 테스트 스크립트의 default function에서 호출하세요.
 */
export function createReservation() {
  const phoneNumber = generateUniquePhone();

  const payload = JSON.stringify({
    scheduleId: SCHEDULE_ID,
    guestName: `tester_${__VU}_${__ITER}`,
    guestPhoneNumber: phoneNumber,
    ticketCount: 1,
    answers: [],
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Test-Id': TEST_ID,
      'X-Perf-Test': 'true',
    },
    tags: { name: '/api/reservations' },
  };

  const startTime = Date.now();
  const response = http.post(`${BASE_URL}/api/reservations`, payload, params);
  const duration = Date.now() - startTime;

  reservationDuration.add(duration);

  const success = check(response, {
    'status is 201': (r) => r.status === 201,
    'has reservation id': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.id !== undefined;
      } catch {
        return false;
      }
    },
    'has qrToken': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.qrToken !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (success) {
    reservationSuccess.add(1);
    errorRate.add(0);
  } else {
    reservationFailed.add(1);
    errorRate.add(1);
    console.log(`[VU ${__VU}] Failed: ${response.status} - ${response.body}`);
  }

  sleep(0.5);
}
