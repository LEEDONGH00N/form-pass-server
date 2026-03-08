/**
 * ===========================================
 * 비관적 락 한계점 측정 — 티켓팅 스파이크 스트레스 테스트
 * ===========================================
 *
 * 목적:
 *   티켓팅 오픈 직후 트래픽이 한순간에 몰리는 상황을 재현하여
 *   비관적 락(PESSIMISTIC_WRITE) 기반 예약 처리의 병목 지점을 찾는다.
 *
 * 시나리오:
 *   setup()에서 /actuator/health로 서버 정상 확인 후,
 *   오픈 직후 동시 사용자가 급증하는 패턴을 재현.
 *   VU를 100 → 200 → 300 → 500으로 올리며 한계점을 탐색.
 *
 * 성공/실패 판정:
 *   - 201 (예약 성공): 정상
 *   - 409 (정원 초과 / 중복 예약): 정상 (시스템이 올바르게 방어)
 *   - 5xx (서버 에러 / 타임아웃): 실패 (비관적 락 병목)
 *
 * 실행 방법:
 *   ./perf.sh --env production --script reservation-stress-test.js --dashboard
 *
 * 주의:
 *   테스트 전 대상 스케줄의 maxCapacity를 충분히 높여야 합니다. (최소 10,000 이상)
 *   좌석 부족으로 인한 실패와 락 병목으로 인한 실패를 구분하기 위함.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { BASE_URL, SCHEDULE_ID, TEST_ID, generateUniquePhone } from './reservation-helpers.js';

// ============================================
// Custom Metrics
// ============================================
const reservationCreated = new Counter('reservation_created');   // 201 — 예약 성공
const reservationRejected = new Counter('reservation_rejected'); // 409 — 정원 초과/중복 (정상 방어)
const serverError = new Counter('server_error');                 // 5xx — 병목 발생
const serverErrorRate = new Rate('server_error_rate');            // 5xx 비율 — 핵심 지표
const responseDuration = new Trend('reservation_duration');

export const options = {
  stages: [
    // 티켓팅 오픈 — 급증
    { duration: '5s', target: 100 },
    { duration: '30s', target: 100 },

    // 트래픽 증가
    { duration: '5s', target: 200 },
    { duration: '30s', target: 200 },

    // 고부하
    { duration: '5s', target: 300 },
    { duration: '30s', target: 300 },

    // 극한
    { duration: '5s', target: 500 },
    { duration: '30s', target: 500 },
  ],

  thresholds: {
    http_req_duration: ['p(95)<10000'],
    server_error_rate: ['rate<1.0'],
  },

  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],
};

// 본 테스트 전 서버 상태 확인
export function setup() {
  const res = http.get(`${BASE_URL}/actuator/health`);
  const healthy = check(res, {
    'server is healthy': (r) => r.status === 200,
  });

  if (!healthy) {
    throw new Error(`서버 헬스체크 실패: ${res.status} - ${res.body}`);
  }

  console.log(`서버 정상 확인 — 스트레스 테스트 시작`);
}

export default function () {
  const phoneNumber = generateUniquePhone();

  const payload = JSON.stringify({
    scheduleId: SCHEDULE_ID,
    guestName: `stress_${__VU}_${__ITER}`,
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
  const res = http.post(`${BASE_URL}/api/reservations`, payload, params);
  responseDuration.add(Date.now() - startTime);

  if (res.status === 201) {
    reservationCreated.add(1);
    serverErrorRate.add(0);
  } else if (res.status === 409 || res.status === 400) {
    // 정원 초과, 중복 예약 — 시스템이 정상적으로 거부
    reservationRejected.add(1);
    serverErrorRate.add(0);
  } else {
    // 5xx, 타임아웃 — 비관적 락 병목
    serverError.add(1);
    serverErrorRate.add(1);
    console.log(`[VU ${__VU}] SERVER ERROR: ${res.status} - ${res.body}`);
  }

  sleep(0.5);
}
