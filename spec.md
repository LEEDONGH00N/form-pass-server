# Form-PASS 프로젝트 문서 인덱스

프로젝트의 기능 명세, 의사결정 기록, 성능 리포트를 다루는 문서 인덱스입니다.

## 기능 명세

| 문서 | 설명 |
|------|------|
| [API 명세](docs/spec/api-spec.md) | 전체 엔드포인트, 요청/응답 형식, 상태 코드, 인증 방식 |
| [ERD](docs/spec/erd-spec.md) | 테이블 스키마, 관계, 제약조건, 인덱스, Enum 정의 |
| [비즈니스 규칙](docs/spec/business-rules-spec.md) | 예약 생성/취소/체크인 조건, 동시성 제어, 인증/인가 정책 |
| [도메인 상세](docs/spec/domain-spec.md) | 엔티티 클래스, DTO 구조, Repository 쿼리 메서드 |

## PRD (Product Requirements Document)

| 문서 | 상태 | 설명 |
|------|------|------|
| [PRD-001: 이벤트 예약 시스템](docs/prd/prd-001-reservation-system.md) | Released | 핵심 예약 플로우, 유스케이스, 비기능 요구사항 |

## ADR (Architecture Decision Record)

| 문서 | 상태 | 설명 |
|------|------|------|
| [ADR-001: 비관적 락 선택](docs/adr/adr-001-pessimistic-lock.md) | Superseded | 낙관적/비관적/분산 락 비교 및 선택 근거, 한계 측정 결과 |
| [ADR-002: QueryDSL 도입](docs/adr/adr-002-querydsl.md) | Accepted | JPQL 4-way 분기 문제 해결, 선택적 적용 기준 |
| [ADR-003: 분산 락 도입](docs/adr/adr-003-distributed-lock.md) | Proposed | 비관적 락 한계 → Redis + Redisson 분산 락 전환 결정 |

## 리포트

| 문서 | 설명 |
|------|------|
| [부하 테스트 가이드](docs/reports/load-test-guide.md) | k6 스크립트 작성·실행 방법 |
| [Ramp-up 테스트 리포트](docs/reports/load-test-report.md) | 100 VU 피크 부하 테스트 결과 |
| [Constant 테스트 리포트](docs/reports/constant-test-report.md) | 50 VU 고정 부하 테스트 결과 |
| [스트레스 테스트 리포트](docs/reports/stress-test-report-20260307.md) | 500 VU 비관적 락 한계점 측정 |

## 로드맵

| 문서 | 설명 |
|------|------|
| [향후 개발 로드맵](docs/roadmap.md) | 분산 락, 대기열, 서비스 개선, 인프라, 문서 체계 계획 |
