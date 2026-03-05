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
BASE_URL="https://api.form-pass.life"
DENYLIST="/api/auth/email/send,/api/auth/email/verify,/api/host/s3/presigned-url"
ENDPOINTS=""
TOKEN=""
TEST_ID=""
WARMUP=false
DASHBOARD=false

# ============================================
# Parse Arguments
# ============================================
while [[ $# -gt 0 ]]; do
  case "$1" in
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
    --warmup)
      WARMUP=true; shift ;;
    --dashboard)
      DASHBOARD=true; shift ;;
    -h|--help)
      echo "Usage: ./perf.sh [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --script <file>       k6 스크립트 파일명 (기본: common.js)"
      echo "  --base-url <url>      API 서버 URL (기본: https://api.form-pass.life)"
      echo "  --endpoints <csv>     테스트할 GET 엔드포인트 CSV (common.js 전용)"
      echo "  --denylist <csv>      차단할 엔드포인트 CSV"
      echo "  --token <token>       Bearer 토큰"
      echo "  --test-id <id>        테스트 ID (미지정 시 자동 생성)"
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
echo " Script    : ${SCRIPT}"
echo " Base URL  : ${BASE_URL}"
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
