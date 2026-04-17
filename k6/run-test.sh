#!/bin/bash
# 사용법:
#   ./k6/run-test.sh "라벨" BASE_URL VUS QUANTITY
# 예:
#   ./k6/run-test.sh "Gateway atomic async (1000 VU)" http://localhost:9000 1000 200
#
# 동작:
#   1. DB 초기화 (coupon-api 재시작 → create-drop)
#   2. pre-seed 유저 생성
#   3. 쿠폰 생성 (외부에서 curl)
#   4. k6 실행 (COUPON_ID 환경변수로 전달)
#   5. k6가 k6/last-run.md에 기록한 결과를 docs/history/load_test_results.md에 append

set -e

LABEL="$1"
BASE_URL="$2"
VUS="$3"
QUANTITY="${4:-200}"

if [ -z "$LABEL" ] || [ -z "$BASE_URL" ] || [ -z "$VUS" ]; then
    echo "사용법: $0 \"라벨\" BASE_URL VUS [QUANTITY]"
    exit 1
fi

# BASE_URL 체크
if ! curl -s -o /dev/null --max-time 2 "$BASE_URL" 2>/dev/null; then
    echo "경고: $BASE_URL 응답 없음. 서버 확인 필요."
fi

echo "===== 1. DB 초기화 (coupon-api 재시작) ====="
docker compose restart coupon-api-1 coupon-api-2 > /dev/null
sleep 20

# 실제 쓰기 요청으로 헬스체크 (DDL create-drop이 끝났는지 확인)
for i in {1..30}; do
    HEALTH_RES=$(curl -s -w "%{http_code}" -o /tmp/health_body \
        -X POST "${BASE_URL}/api/users" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"health$i@t.com\",\"name\":\"h\"}" 2>/dev/null)
    if [ "$HEALTH_RES" = "200" ]; then
        echo "서버 기동 완료 ($i회 시도)"
        break
    fi
    echo "서버 기동 대기... ($i/30, status=$HEALTH_RES)"
    sleep 2
done

echo ""
echo "===== 2. 유저 $VUS 명 pre-seed ====="
BASE_URL="$BASE_URL" USERS="$VUS" k6 run k6/seed-users.js --quiet

echo ""
echo "===== 3. 쿠폰 생성 ====="
COUPON_ID=$(curl -s -X POST "${BASE_URL}/api/coupons" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"k6쿠폰_$(date +%s)\",
        \"description\": \"k6 부하 테스트\",
        \"pointCost\": 100,
        \"totalQuantity\": $QUANTITY,
        \"startDate\": \"2026-01-01T00:00:00\",
        \"endDate\": \"2027-01-01T00:00:00\"
    }" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo "couponId=$COUPON_ID"

echo ""
echo "===== 4. 메트릭 수집 시작 (백그라운드) ====="
METRICS_CSV="k6/metrics/run_$(date +%Y%m%d_%H%M%S)_$(echo "$LABEL" | tr ' /()[]' '_').csv"
# 두 서버 중 8080만 수집 (Gateway 테스트도 두 서버에 분산되지만 peak는 비슷)
./k6/metrics-collector.sh "$METRICS_CSV" "http://localhost:8080" 1 > /dev/null 2>&1 &
METRICS_PID=$!
echo "metrics collector PID=$METRICS_PID, CSV=$METRICS_CSV"
sleep 2  # 초기 baseline 샘플

echo ""
echo "===== 5. k6 부하 테스트 실행 ====="
LABEL="$LABEL" BASE_URL="$BASE_URL" VUS="$VUS" QUANTITY="$QUANTITY" COUPON_ID="$COUPON_ID" \
    k6 run k6/load-test.js

echo ""
echo "===== 6. 메트릭 수집 종료 ====="
sleep 2  # 종료 후 tail 샘플
kill $METRICS_PID 2>/dev/null || true

echo ""
echo "===== 7. 결과 append → docs/history/load_test_results.md ====="
cat k6/last-run.md >> docs/history/load_test_results.md
echo "" >> docs/history/load_test_results.md
./k6/metrics-summary.sh "$METRICS_CSV" >> docs/history/load_test_results.md
echo "" >> docs/history/load_test_results.md
echo "> 시계열 CSV: \`$METRICS_CSV\`" >> docs/history/load_test_results.md
echo "기록 완료. 메트릭 CSV: $METRICS_CSV"
