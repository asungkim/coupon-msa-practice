#!/bin/bash
# Actuator 메트릭을 주기적으로 수집해 CSV 파일에 기록한다.
#
# 사용법:
#   ./k6/metrics-collector.sh <출력CSV> <ACTUATOR_BASE_URL> [INTERVAL_SEC]
# 예:
#   ./k6/metrics-collector.sh k6/metrics/run_$(date +%s).csv http://localhost:8080 1
#
# 정지:
#   Ctrl+C 또는 백그라운드 실행 시 kill $PID

set -e

OUT_CSV="$1"
ACTUATOR_URL="$2"
INTERVAL="${3:-1}"

if [ -z "$OUT_CSV" ] || [ -z "$ACTUATOR_URL" ]; then
    echo "사용법: $0 <출력CSV> <ACTUATOR_URL> [INTERVAL_SEC]"
    exit 1
fi

mkdir -p "$(dirname "$OUT_CSV")"

# actuator 메트릭에서 특정 값을 추출하는 함수
fetch_metric() {
    local name="$1"
    local tag="$2"  # 선택적 태그 필터 (예: "state:active")
    local url="${ACTUATOR_URL}/actuator/metrics/${name}"
    if [ -n "$tag" ]; then
        url="${url}?tag=${tag}"
    fi
    curl -s --max-time 2 "$url" \
        | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d['measurements'][0]['value'])
except Exception:
    print('')
" 2>/dev/null
}

# CSV 헤더
echo "timestamp,cpu_process,cpu_system,heap_used_mb,heap_max_mb,threads_live,hikari_active,hikari_idle,hikari_pending,hikari_timeout_total" > "$OUT_CSV"

echo ">>> 메트릭 수집 시작: $OUT_CSV (${INTERVAL}초 간격)"
echo ">>> 대상: $ACTUATOR_URL"

while true; do
    ts=$(date +"%H:%M:%S.%3N")

    cpu_process=$(fetch_metric "process.cpu.usage")
    cpu_system=$(fetch_metric "system.cpu.usage")
    heap_used=$(fetch_metric "jvm.memory.used" "area:heap")
    heap_max=$(fetch_metric "jvm.memory.max" "area:heap")
    threads=$(fetch_metric "jvm.threads.live")
    hikari_active=$(fetch_metric "hikaricp.connections.active")
    hikari_idle=$(fetch_metric "hikaricp.connections.idle")
    hikari_pending=$(fetch_metric "hikaricp.connections.pending")
    hikari_timeout=$(fetch_metric "hikaricp.connections.timeout")

    # bytes → MB 변환
    heap_used_mb=$(python3 -c "v='$heap_used'; print(round(float(v)/1024/1024, 1) if v else '')" 2>/dev/null)
    heap_max_mb=$(python3 -c "v='$heap_max'; print(round(float(v)/1024/1024, 1) if v else '')" 2>/dev/null)

    echo "$ts,$cpu_process,$cpu_system,$heap_used_mb,$heap_max_mb,$threads,$hikari_active,$hikari_idle,$hikari_pending,$hikari_timeout" >> "$OUT_CSV"

    sleep "$INTERVAL"
done
