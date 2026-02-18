# 모니터링 & 부하테스트 서버 설정 가이드

## 인프라 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                         AWS VPC                                  │
│                                                                  │
│  ┌─────────────────────────┐    ┌─────────────────────────┐     │
│  │     운영 서버 (t2.micro) │    │  모니터링 서버 (t2.micro)│     │
│  │                         │    │                         │     │
│  │  ┌─────────────────┐   │    │  ┌─────────────────┐   │     │
│  │  │     Nginx       │   │    │  │   Prometheus    │   │     │
│  │  │  (HTTPS/443)    │   │    │  │    (:9090)      │   │     │
│  │  └────────┬────────┘   │    │  └────────┬────────┘   │     │
│  │           │            │    │           │            │     │
│  │  ┌────────▼────────┐   │    │  ┌────────▼────────┐   │     │
│  │  │  Spring Boot    │◄──┼────┼──│    Grafana      │   │     │
│  │  │    (:8080)      │   │    │  │    (:3000)      │   │     │
│  │  │                 │   │    │  └─────────────────┘   │     │
│  │  │ /actuator/      │◄──┼────┼─── 메트릭 수집 (15초)   │     │
│  │  │ prometheus      │   │    │                         │     │
│  │  └─────────────────┘   │    │  ┌─────────────────┐   │     │
│  │                         │    │  │       k6        │   │     │
│  │                         │    │  │   (부하테스트)    │   │     │
│  └─────────────────────────┘    │  └─────────────────┘   │     │
│                                  └─────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

### 데이터 흐름

| 흐름 | 설명 |
|------|------|
| 사용자 → 운영서버 | HTTPS → Nginx → Spring Boot |
| Prometheus → 운영서버 | 15초마다 `/actuator/prometheus` 메트릭 수집 |
| k6 → 운영서버 | 부하테스트 요청 전송 |
| k6 → Prometheus | 테스트 결과 메트릭 전송 |
| Grafana → Prometheus | 메트릭 조회 → 시각화 |

### 포트 정리

| 서버 | 포트 | 용도 |
|------|------|------|
| 운영 | 443 | HTTPS (외부) |
| 운영 | 8080 | Spring Boot (내부) |
| 모니터링 | 3000 | Grafana |
| 모니터링 | 9090 | Prometheus |
| 모니터링 | 5665 | k6 대시보드 (테스트 중) |

---

## 사전 준비

- Ubuntu EC2 인스턴스 (t2.micro + 2GB 스왑 메모리)
- 운영 서버와 같은 VPC 내 위치

---

## 1. Docker 설치

```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER
exit
```

재접속 후 계속 진행.

---

## 2. Docker Compose 설치

```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

---

## 3. 설정 파일 생성

```bash
mkdir ~/monitoring && cd ~/monitoring
```

### prometheus.yml

```bash
cat > prometheus.yml << 'EOF'
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['운영서버_프라이빗IP:8080']
EOF
```

> `운영서버_프라이빗IP`를 실제 운영 서버의 프라이빗 IP로 변경 (예: `172.31.1.123`)

### docker-compose.yml

```bash
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--web.enable-remote-write-receiver'
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=${GF_ADMIN_USER}
      - GF_SECURITY_ADMIN_PASSWORD=${GF_ADMIN_PASSWORD}
    volumes:
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
    restart: unless-stopped

volumes:
  grafana-data:
EOF
```

### .env

```bash
cat > .env << 'EOF'
GF_ADMIN_USER=admin
GF_ADMIN_PASSWORD=변경할비밀번호
EOF
```

---

## 4. 컨테이너 실행

```bash
docker-compose up -d
```

### 확인 명령어

```bash
# 컨테이너 상태
docker ps

# 메모리 사용량
docker stats --no-stream

# Prometheus 타겟 확인
curl http://localhost:9090/api/v1/targets
```

---

## 5. k6 설치

```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt update
sudo apt install -y k6
```

```bash
# 버전 확인
k6 version
```

---

## 6. AWS 보안 그룹 설정

### 모니터링 서버

| 포트 | 용도 | 소스 |
|------|------|------|
| 3000 | Grafana | 본인 IP |
| 9090 | Prometheus | 본인 IP |
| 5665 | k6 대시보드 | 본인 IP |

### 운영 서버

| 포트 | 용도 | 소스 |
|------|------|------|
| 8080 | Actuator 메트릭 | 모니터링 서버 프라이빗 IP |

---

## 7. Grafana 설정

### 접속

`http://모니터링서버_퍼블릭IP:3000`

### Data Source 추가

1. 좌측 메뉴 → Connections → Data sources
2. Add data source → Prometheus 선택
3. URL: `http://prometheus:9090`
4. Save & Test

### Spring Boot 대시보드 추가

1. Dashboards → Import
2. ID: `11378` → Load
3. Prometheus 선택 → Import

### k6 대시보드 추가

1. Dashboards → Import
2. ID: `19665` → Load
3. Prometheus 선택 → Import

---

## 8. k6 부하테스트

### 테스트 스크립트 작성

```bash
cat > ~/test.js << 'EOF'
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 10 },   // 10초간 10명까지 증가
    { duration: '20s', target: 10 },   // 20초간 10명 유지
    { duration: '10s', target: 0 },    // 10초간 0명으로 감소
  ],
};

export default function () {
  const res = http.get('https://www.form-pass.life/api/events');
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(1);
}
EOF
```

### 실행 방법

**CLI 결과만 보기:**
```bash
k6 run ~/test.js
```

**실시간 웹 대시보드:**
```bash
k6 run --out web-dashboard=host=0.0.0.0&port=5665 ~/test.js
```
→ `http://모니터링서버IP:5665` 접속

**Grafana로 결과 전송:**
```bash
k6 run --out experimental-prometheus-rw=http://localhost:9090/api/v1/write ~/test.js
```
→ Grafana k6 대시보드에서 확인

---

## 9. 주요 메트릭

### Spring Boot 메트릭

| 메트릭 | 설명 |
|--------|------|
| `http_server_requests_seconds` | API 응답 시간 |
| `jvm_memory_used_bytes` | JVM 메모리 사용량 |
| `hikaricp_connections_active` | DB 커넥션 수 |
| `system_cpu_usage` | CPU 사용률 |
| `tomcat_threads_busy_threads` | 톰캣 쓰레드 |

### k6 메트릭

| 메트릭 | 설명 |
|--------|------|
| `k6_http_reqs_total` | 총 요청 수 |
| `k6_http_req_duration_seconds` | 응답 시간 |
| `k6_http_req_failed_total` | 실패한 요청 |
| `k6_vus` | 현재 가상 사용자 수 |
| `k6_iterations_total` | 반복 횟수 |

---

## 10. 문제 해결

### Prometheus 타겟 DOWN

1. 운영 서버에서 확인:
```bash
curl http://localhost:8080/actuator/prometheus
```

2. 모니터링 서버에서 연결 확인:
```bash
curl http://운영서버_프라이빗IP:8080/actuator/prometheus
```

3. 안되면 운영 서버 보안 그룹에서 8080 포트 열기

### Grafana 접속 안됨

- 보안 그룹에서 3000 포트 확인
- `docker ps`로 컨테이너 실행 확인

### k6 대시보드 접속 안됨

- 테스트 실행 중인지 확인 (종료되면 대시보드도 종료)
- 보안 그룹에서 5665 포트 확인
- `ss -tlnp | grep 5665`로 포트 열렸는지 확인
