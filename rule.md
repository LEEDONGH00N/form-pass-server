# AI Coding Agent Rules for Backend Project

이 문서는 프로젝트의 백엔드 아키텍처, 코드 컨벤션, 비즈니스 제약 사항을 정의합니다. 코드를 작성하거나 수정하기 전에 반드시 아래 규칙을 숙지하고 엄격하게 준수하십시오.

## 1. Tech Stack & Core Convention
- **Framework:** Spring Boot (Java/Kotlin)
- **Dependency Injection:** `@Autowired` 사용을 엄격히 금지합니다. 반드시 Lombok의 `@RequiredArgsConstructor`를 이용한 생성자 주입(Constructor Injection)을 사용하십시오.
- **DTO (Data Transfer Object):** Entity 클래스를 API 요청/응답 객체로 절대 직접 노출하지 마십시오. Controller와 Service 계층 간의 데이터 교환은 반드시 전용 DTO 또는 Record 클래스를 생성하여 매핑해야 합니다.
- **Exception Handling:** 개별 메서드나 컨트롤러에서 `try-catch`로 에러를 처리하지 마십시오. 전역 예외 처리기(`@RestControllerAdvice`)를 통해 Custom Exception을 던지고, 일관된 에러 응답 스펙을 유지하십시오.

## 2. Database, JPA & Query Optimization
- **Lazy Loading 기본화:** 모든 JPA 연관관계 매핑(`@OneToMany`, `@ManyToOne` 등)은 반드시 `FetchType.LAZY`로 명시하십시오. `LazyInitializationException`이 발생하지 않도록 주의하십시오.
- **N+1 문제 방지:** 컬렉션이나 연관된 엔티티를 조회할 때는 단순 `findAll()`을 지양하고, 반드시 `Fetch Join` 또는 `IN` 쿼리를 적절히 혼합하여 N+1 문제를 원천 차단하십시오.
- **인덱스(Index) 고려:** DB 테이블이나 스키마 설계 시, 혹은 복잡한 조회 쿼리를 작성할 때는 클러스터링 인덱스 및 커버링 인덱스(Covering Index) 활용 가능성을 고려하여 쿼리를 최적화하십시오.
- **동시성 제어 (Concurrency):** 이벤트 생성, 예약(Reservation) 등 동시성 이슈가 발생할 수 있는 비즈니스 로직에서는 데이터베이스의 비관적 락(Pessimistic Locking)을 우선적으로 적용하여 데이터 정합성을 보장하십시오.

## 3. Domain & Architecture Constraints
- **회원 통합 시스템 (Member Integration):** 시스템에는 `SUPER_ADMIN`, `AGENCY`, `VISITOR` 등의 다양한 권한(Role)이 존재하며, 이는 단일 `Member` 엔티티로 통합되어 관리됩니다. 권한 검증 로직은 하드코딩된 `if-else` 분기 대신, 시큐리티 컨텍스트나 명확한 권한 검증 계층을 통해 처리하십시오.
- **레거시 호환성 (Legacy Compatibility):** 기존 시스템과의 하위 호환성을 유지해야 합니다. 기존 데이터베이스 스키마나 공통 API 응답 규격을 임의로 변경하지 마십시오.
- **비동기 및 이벤트 처리:** `@Async` 또는 `@TransactionalEventListener`를 사용할 때는 트랜잭션의 커밋 시점을 명확히 인지하고, 메인 트랜잭션 롤백 시 발생할 수 있는 사이드 이펙트를 주석으로 명시하십시오. Session 관리 로직과 충돌하지 않도록 주의하십시오.

## 4. DevOps & Infrastructure
- **모니터링 친화적 코드:** 향후 Prometheus, Grafana, Loki, APM 등의 도구와 k6 부하 테스트를 통한 모니터링이 용이하도록, 주요 비즈니스 로직에 적절한 로그 레벨(Info, Warn, Error)을 적용하여 명확한 추적(Trace)이 가능하게 작성하십시오.

## 5. AI Behavior Rules
- **선 제안, 후 작성:** 복잡한 아키텍처 설계, 대규모 리팩토링, 또는 성능 최적화가 필요한 경우 코드를 바로 생성하지 마십시오. 구현 전략을 3줄 이내로 먼저 설명하고 사용자의 승인을 받은 후 작성하십시오.
- **주석 및 TODO 보존:** 기존 코드에 작성된 한국어 주석이나 `TODO`, `FIXME` 마커는 리팩토링 중에도 절대 삭제하거나 임의로 변경하지 마십시오.
- **최소 범위 수정:** 파일 전체를 재작성하지 말고, 목적을 달성할 수 있는 최소한의 라인과 메서드만 수정하십시오.