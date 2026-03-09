# Logback + Loki 로깅 시스템 구축 계획

> 작성일: 2026-03-09
> 상태: 계획

---

## 1. 목표

애플리케이션 로그를 중앙 수집하여 Grafana에서 **메트릭(Prometheus)과 로그(Loki)를 통합 조회**할 수 있는 환경을 구축한다.

### 현재

```
Spring Boot → Logback → 파일/콘솔 출력 (서버 접속해서 tail -f로 확인)
Prometheus → Grafana (메트릭만 시각화)
```

### 목표

```
Spring Boot → Logback → loki-logback-appender → Loki → Grafana (로그 시각화)
Spring Boot → Micrometer → Prometheus → Grafana (메트릭 시각화)
```

---

## 2. 기술 스택

| 구성 요소 | 역할 | 비고 |
|-----------|------|------|
| **Logback** | 애플리케이션 로깅 프레임워크 | Spring Boot 기본 내장 |
| **loki-logback-appender** | Logback → Loki 전송 | Grafana 공식 라이브러리 |
| **Loki** | 로그 수집·저장·검색 엔진 | Prometheus와 유사한 라벨 기반 |
| **Grafana** | 통합 대시보드 | 기존 Prometheus + Loki 데이터소스 추가 |

---

## 3. 인프라 구성

```
EC2 ① - 애플리케이션 서버 (t3.small)
├── Nginx            (:80, :443)    → 리버스 프록시, SSL 종료
├── Spring Boot      (:8080)        → 애플리케이션
│     ├─ Micrometer ──────────────────→ Prometheus (메트릭 scrape)
│     └─ loki-logback-appender ───────→ Loki (로그 HTTP push)
└── Redis            (:6379)        → 분산 락

EC2 ② - 모니터링 서버
├── Prometheus       (:9090)        → 메트릭 수집·저장
├── Loki             (:3100)        → 로그 수집·저장
└── Grafana          (:3000)        → 통합 대시보드
      ├─ Prometheus 데이터소스 ← 메트릭 시각화
      └─ Loki 데이터소스 ← 로그 시각화

RDS MySQL            (:3306)        → 데이터 저장
```

- 애플리케이션 서버 → 모니터링 서버: 내부 네트워크(VPC) 통신
- Spring Boot → Loki: HTTP POST (loki-logback-appender)
- Prometheus → Spring Boot: HTTP GET scrape (/actuator/prometheus)

---

## 4. 구현 단계

### Phase 1: Loki 설치 (모니터링 서버)

모니터링 서버(EC2 ②)에 Loki 바이너리 설치 및 systemd 서비스 등록.

```bash
# 바이너리 다운로드
curl -O -L "https://github.com/grafana/loki/releases/latest/download/loki-linux-amd64.zip"
unzip loki-linux-amd64.zip

# 설정 파일 생성
# /etc/loki/loki-config.yml

# systemd 서비스 등록
sudo systemctl enable loki
sudo systemctl start loki
```

Loki 기본 설정:
- 포트: 3100
- 스토리지: 로컬 파일시스템 (`/var/loki/`)
- 보존 기간: 7일 (디스크 절약)

### Phase 2: Spring Boot 연동

**build.gradle 의존성 추가:**

```groovy
implementation 'com.github.loki4j:loki-logback-appender:1.5.1'
```

**logback-spring.xml 생성:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 콘솔 출력 (기존 유지) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Loki 전송 -->
    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>http://{모니터링서버IP}:3100/loki/api/v1/push</url>
        </http>
        <format>
            <label>
                <pattern>app=form-pass,host=${HOSTNAME},level=%level</pattern>
            </label>
            <message>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </message>
        </format>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="LOKI" />
    </root>
</configuration>
```

핵심 라벨:
- `app`: 애플리케이션 이름
- `host`: 서버 호스트명
- `level`: 로그 레벨 (INFO, WARN, ERROR)

### Phase 3: 구조화된 로그 적용

주요 이벤트에 구조화된 로그를 추가하여 Loki에서 검색·필터링할 수 있도록 한다.

**DistributedLockService — 락 실패 로그:**

```java
log.warn("[LOCK_TIMEOUT] key={}, waitTime={}s", lockKey, WAIT_TIME_SECONDS);
```

**ReservationService — 예약 성공 로그:**

```java
log.info("[RESERVATION_CREATED] scheduleId={}, reservationId={}", scheduleId, reservationId);
```

**GlobalExceptionHandler — 비즈니스 예외 로그:**

```java
log.warn("[BIZ_EXCEPTION] type={}, status={}, message={}",
    e.getClass().getSimpleName(), e.getHttpStatus(), e.getMessage());
```

로그 태그 규칙:
- `[LOCK_TIMEOUT]` — 분산 락 대기 초과
- `[LOCK_INTERRUPTED]` — 락 획득 중 인터럽트
- `[RESERVATION_CREATED]` — 예약 생성 성공
- `[RESERVATION_CANCELLED]` — 예약 취소
- `[BIZ_EXCEPTION]` — 비즈니스 예외 (400, 409, 429 등)
- `[UNEXPECTED_ERROR]` — 예상치 못한 서버 에러 (500)

### Phase 4: Grafana 대시보드 구성

**Loki 데이터소스 추가:**

```
Grafana → Configuration → Data Sources → Add → Loki
URL: http://{모니터링서버IP}:3100
```

**대시보드 패널 구성:**

| 패널 | LogQL 쿼리 | 용도 |
|------|-----------|------|
| 429 발생 추이 | `rate({app="form-pass"} \|= "[LOCK_TIMEOUT]" [5m])` | 락 실패 빈도 |
| 에러 로그 | `{app="form-pass", level="ERROR"}` | 서버 에러 실시간 확인 |
| 예약 성공 추이 | `rate({app="form-pass"} \|= "[RESERVATION_CREATED]" [5m])` | 예약 처리량 |
| 비즈니스 예외 | `{app="form-pass"} \|= "[BIZ_EXCEPTION]"` | 400/409/429 상세 |

**메트릭 + 로그 연동 예시:**

```
Grafana 대시보드
├── 상단: Prometheus — HTTP 요청 응답시간 그래프
├── 중단: Prometheus — DB 커넥션 풀 사용량
└── 하단: Loki — 같은 시간대의 에러 로그
→ "p95가 튄 시점"을 클릭하면 해당 시간대 로그가 바로 보임
```

### Phase 5: 알림 설정

**Grafana Alert Rules:**

| 알림 조건 | 기준 | 알림 채널 |
|-----------|------|----------|
| 429 급증 | 5분간 [LOCK_TIMEOUT] 50건 초과 | Slack / Email |
| 5xx 발생 | 1분간 ERROR 로그 10건 초과 | Slack / Email |
| 로그 수집 중단 | 5분간 로그 0건 | Slack / Email |

---

## 5. 리소스 예상

**애플리케이션 서버 (EC2 ①):**

| 구성 요소 | 예상 메모리 |
|-----------|-----------|
| loki-logback-appender | ~5MB (Spring Boot 내) |

애플리케이션 서버에는 appender만 추가되므로 부담 없음.

**모니터링 서버 (EC2 ②):**

| 구성 요소 | 예상 메모리 |
|-----------|-----------|
| Loki | 50~100MB |

Loki 로그 보존 기간을 7일로 설정하여 디스크 사용도 제한.

---

## 6. 파일 변경 요약

| 작업 | 파일 |
|------|------|
| CREATE | `src/main/resources/logback-spring.xml` |
| MODIFY | `build.gradle` (loki-logback-appender 의존성) |
| MODIFY | `DistributedLockService.java` (구조화된 로그 추가) |
| MODIFY | `GlobalExceptionHandler.java` (구조화된 로그 추가) |
| MODIFY | `ReservationService.java` (예약 성공/취소 로그 추가) |
| CREATE | 모니터링 서버: `/etc/loki/loki-config.yml` |
| CREATE | 모니터링 서버: `/etc/systemd/system/loki.service` |
| MODIFY | Grafana: Loki 데이터소스 추가 + 대시보드 패널 |

---

## 7. 검증

```bash
# 1. Loki 동작 확인
curl http://{모니터링서버IP}:3100/ready
# → ready

# 2. Spring Boot 실행 후 로그 수집 확인
curl -G "http://{모니터링서버IP}:3100/loki/api/v1/query" \
  --data-urlencode 'query={app="form-pass"}' | jq

# 3. Grafana에서 Loki 쿼리 실행
# Explore → Loki → {app="form-pass", level="WARN"}

# 4. 부하 테스트 후 429 로그 확인
# {app="form-pass"} |= "[LOCK_TIMEOUT]"
```
