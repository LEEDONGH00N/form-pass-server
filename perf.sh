#!/usr/bin/env bash
# ===========================================
# Form PASS 부하테스트 실행 래퍼
# ===========================================
#
# 사용 예시:
#   ./perf.sh --endpoints "/api/events,/health" --warmup --dashboard
#   ./perf.sh --script reservation-test.js --warmup --dashboard
#   ./perf.sh --endpoints "/api/host/events" --token "$TOKEN" --dashboard
#
set -euo pipefail

# ============================================
# Defaults
# ============================================
SCRIPT="common.js"
ENV_NAME=""
BASE_URL=""
DENYLIST=""
ENDPOINTS=""
TOKEN=""
TEST_ID=""
WARMUP=false
DASHBOARD=false
SCHEDULE_ID=""

# ============================================
# Parse Arguments
# ============================================
while [[ $# -gt 0 ]]; do
  case "$1" in
    --env)
      ENV_NAME="$2"; shift 2 ;;
    --script)
      SCRIPT="$2"; shift 2 ;;
    --base-url)
      BASE_URL="$2"; shift 2 ;;
    --endpoints)
      ENDPOINTS="$2"; shift 2 ;;
    --denylist)
      DENYLIST="$2"; shift 2 ;;
    --token)
      TOKEN="$2"; shift 2 ;;
    --test-id)
      TEST_ID="$2"; shift 2 ;;
    --schedule-id)
      SCHEDULE_ID="$2"; shift 2 ;;
    --warmup)
      WARMUP=true; shift ;;
    --dashboard)
      DASHBOARD=true; shift ;;
    -h|--help)
      echo "Usage: ./perf.sh --env <environment> [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --env <name>          환경 파일 (production|local) [필수]"
      echo "  --script <file>       k6 스크립트 파일명 (기본: common.js)"
      echo "  --base-url <url>      API 서버 URL (env 파일보다 우선)"
      echo "  --endpoints <csv>     테스트할 GET 엔드포인트 CSV (common.js 전용)"
      echo "  --denylist <csv>      차단할 엔드포인트 CSV (env 파일보다 우선)"
      echo "  --token <token>       Bearer 토큰"
      echo "  --test-id <id>        테스트 ID (미지정 시 자동 생성)"
      echo "  --schedule-id <id>    테스트 대상 스케줄 ID (env 파일보다 우선)"
      echo "  --warmup              warm-up 실행 후 main 실행 (2회 자동 실행)"
      echo "  --dashboard           웹 대시보드 활성화 (http://localhost:5665)"
      echo "  -h, --help            도움말 출력"
      exit 0 ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1 ;;
  esac
done

# ============================================
# Load Environment File
# ============================================
if [[ -z "$ENV_NAME" ]]; then
  echo "❌ --env 옵션은 필수입니다. (production|local)"
  echo "   예: ./perf.sh --env production --script reservation-test.js"
  exit 1
fi

ENV_FILE="$(dirname "$0")/k6/env/${ENV_NAME}.env"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "❌ 환경 파일을 찾을 수 없습니다: ${ENV_FILE}"
  echo "   사용 가능한 환경: $(ls k6/env/*.env 2>/dev/null | xargs -I{} basename {} .env | tr '\n' ', ')"
  exit 1
fi

# env 파일에서 기본값 로드 (CLI 옵션이 우선)
while IFS='=' read -r key value; do
  [[ -z "$key" || "$key" =~ ^# ]] && continue
  value="${value%%#*}"    # 인라인 주석 제거
  value="${value%"${value##*[! ]}"}"  # 후행 공백 제거
  case "$key" in
    BASE_URL)     [[ -z "$BASE_URL" ]] && BASE_URL="$value" ;;
    SCHEDULE_ID)  [[ -z "$SCHEDULE_ID" ]] && SCHEDULE_ID="$value" ;;
    DENYLIST)     [[ -z "$DENYLIST" ]] && DENYLIST="$value" ;;
  esac
done < "$ENV_FILE"

# 최종 기본값 적용
BASE_URL="${BASE_URL:-https://api.form-pass.life}"
DENYLIST="${DENYLIST:-/api/auth/email/send,/api/auth/email/verify,/api/host/s3/presigned-url}"
SCHEDULE_ID="${SCHEDULE_ID:-1}"

# ============================================
# Auto-generate Test ID
# ============================================
if [[ -z "$TEST_ID" ]]; then
  TEST_ID="exp-$(date +%Y%m%d-%H%M)"
fi

# ============================================
# Docker arguments
# ============================================
build_docker_args() {
  local warmup_flag="$1"
  local suffix="$2"
  local current_test_id="${TEST_ID}-${suffix}"

  local args=(
    "docker" "run" "--rm"
    "-e" "BASE_URL=${BASE_URL}"
    "-e" "TEST_ID=${current_test_id}"
    "-e" "DENYLIST=${DENYLIST}"
    "-e" "SCHEDULE_ID=${SCHEDULE_ID}"
  )

  if [[ -n "$ENDPOINTS" ]]; then
    args+=("-e" "ENDPOINTS=${ENDPOINTS}")
  fi

  if [[ -n "$TOKEN" ]]; then
    args+=("-e" "TOKEN=${TOKEN}")
  fi

  if [[ "$warmup_flag" == "true" ]]; then
    args+=("-e" "WARMUP=true")
  fi

  if [[ "$DASHBOARD" == true ]]; then
    args+=("-p" "5665:5665")
  fi

  args+=("-v" "$(pwd)/k6:/scripts")
  args+=("grafana/k6" "run")

  if [[ "$DASHBOARD" == true ]]; then
    args+=("--out" "web-dashboard=host=0.0.0.0")
  fi

  args+=("/scripts/${SCRIPT}")

  echo "${args[@]}"
}

# ============================================
# Print Summary
# ============================================
echo "========================================"
echo " Form PASS 부하테스트"
echo "========================================"
echo " Env       : ${ENV_NAME}"
echo " Script    : ${SCRIPT}"
echo " Base URL  : ${BASE_URL}"
echo " Schedule  : ${SCHEDULE_ID}"
echo " Test ID   : ${TEST_ID}"
echo " Warmup    : ${WARMUP}"
echo " Dashboard : ${DASHBOARD}"
if [[ -n "$ENDPOINTS" ]]; then
  echo " Endpoints : ${ENDPOINTS}"
fi
echo " Denylist  : ${DENYLIST}"
echo " Token     : ${TOKEN:+(set)}"
echo "========================================"
echo ""

# ============================================
# Execute
# ============================================
if [[ "$WARMUP" == true ]]; then
  echo "[1/2] Warm-up 실행 (Test ID: ${TEST_ID}-warmup)"
  echo "----------------------------------------"
  eval "$(build_docker_args true warmup)"

  echo ""
  echo "Warm-up 완료. 3초 대기 후 main 실행..."
  sleep 3
  echo ""

  echo "[2/2] Main 실행 (Test ID: ${TEST_ID}-main)"
  echo "----------------------------------------"
  eval "$(build_docker_args false main)"
else
  echo "Main 실행 (Test ID: ${TEST_ID}-main)"
  echo "----------------------------------------"
  eval "$(build_docker_args false main)"
fi

echo ""
echo "========================================"
echo " 테스트 완료: ${TEST_ID}"
echo "========================================"
