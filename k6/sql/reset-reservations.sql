-- 예약 부하 테스트 전 초기화 스크립트
-- 대상 스케줄의 예약 데이터를 삭제하고 reservedCount를 초기화합니다.
-- 사용법: mysql -u {user} -p {database} < k6/sql/reset-reservations.sql

-- 1. 폼 응답 삭제
DELETE FROM form_answers WHERE reservation_id IN (
  SELECT id FROM reservations WHERE event_schedule_id = 1
);

-- 2. 예약 삭제
DELETE FROM reservations WHERE event_schedule_id = 1;

-- 3. 예약 수 초기화 + capacity 확보
UPDATE event_schedules SET reserved_count = 0, max_capacity = 100000 WHERE id = 1;
