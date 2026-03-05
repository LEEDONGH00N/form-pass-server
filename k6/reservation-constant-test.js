/**
 * ===========================================
 * Form PASS 예약 API 부하 테스트 (Constant VU)
 * ===========================================
 *
 * [테스트 시나리오]
 * 고정된 VU로 일정 시간 동안 부하 유지
 * → 정확한 기준 성능(Baseline) 측정 목적
 *
 * [테스트 시간] 2분
 * [동시 사용자] 50 VU (고정)
 *
 * [성공 기준]
 * - p(95) 응답 시간 < 500ms
 * - 에러율 < 1%
 *
 * [실행 방법]
 * $ ./perf.sh --script reservation-constant-test.js --warmup --dashboard
 *
 * [대시보드 확인]
 * http://localhost:5665
 */

import { createReservation, BASE_URL, SCHEDULE_ID, TEST_ID } from './reservation-helpers.js';

// ============================================
// Test Configuration
// ============================================
export const options = {
  vus: 50,
  duration: '2m',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    error_rate: ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],
};

// ============================================
// Main
// ============================================
export default function () {
  createReservation();
}

export function setup() {
  console.log('========================================');
  console.log('Constant VU 부하 테스트 시작');
  console.log(`Test ID     : ${TEST_ID}`);
  console.log(`VU: ${options.vus}, Duration: ${options.duration}`);
  console.log(`Target      : ${BASE_URL}`);
  console.log(`Schedule ID : ${SCHEDULE_ID}`);
  console.log('========================================');
}

export function teardown() {
  console.log('========================================');
  console.log('부하 테스트 완료');
  console.log(`Test ID: ${TEST_ID}`);
  console.log('========================================');
}
