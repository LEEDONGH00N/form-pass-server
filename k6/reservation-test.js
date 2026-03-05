/**
 * ===========================================
 * Form PASS 예약 API 부하 테스트 (Ramp-up/Spike)
 * ===========================================
 *
 * [테스트 시나리오]
 * 1. Ramp-up: 30초 동안 0 → 50 VU 증가
 * 2. Sustain: 1분 동안 50 VU 유지
 * 3. Spike: 30초 동안 50 → 100 VU 증가
 * 4. Peak: 1분 동안 100 VU 유지
 * 5. Ramp-down: 30초 동안 100 → 0 VU 감소
 *
 * [총 테스트 시간] 약 4분
 *
 * [성공 기준]
 * - p(95) 응답 시간 < 2초
 * - 에러율 < 10%
 *
 * [실행 방법]
 * $ ./perf.sh --script reservation-test.js --warmup --dashboard
 *
 * [대시보드 확인]
 * http://localhost:5665
 *
 * [테스트 전 준비]
 * 1. maxCapacity 충분히 설정 (10000 이상)
 * 2. 기존 예약 데이터 삭제:
 *    DELETE FROM form_answers WHERE reservation_id IN (SELECT id FROM reservations WHERE event_schedule_id = 1);
 *    DELETE FROM reservations WHERE event_schedule_id = 1;
 *    UPDATE event_schedules SET reserved_count = 0 WHERE id = 1;
 */

import { createReservation, BASE_URL, SCHEDULE_ID, TEST_ID } from './reservation-helpers.js';

// ============================================
// Test Configuration
// ============================================
export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Ramp-up: 50 VU까지 증가
    { duration: '1m', target: 50 },    // Sustain: 50 VU 유지
    { duration: '30s', target: 100 },  // Spike: 100 VU까지 증가
    { duration: '1m', target: 100 },   // Peak: 100 VU 유지
    { duration: '30s', target: 0 },    // Ramp-down: 0으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    error_rate: ['rate<0.1'],
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
  console.log('Form PASS 예약 API 부하 테스트 시작 (Ramp-up/Spike)');
  console.log(`Test ID     : ${TEST_ID}`);
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
