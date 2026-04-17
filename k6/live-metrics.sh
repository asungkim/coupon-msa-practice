#!/bin/bash
# Actuator 메트릭을 실시간으로 터미널에 표시한다.
#
# 사용법:
#   ./k6/live-metrics.sh [ACTUATOR_URL] [INTERVAL_SEC]
# 예:
#   ./k6/live-metrics.sh http://localhost:8080 1
#   ./k6/live-metrics.sh                          # 기본값: localhost:8080, 1초

set -e

ACTUATOR_URL="${1:-http://localhost:8080}"
INTERVAL="${2:-1}"

# 색상 코드
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

fetch_metric() {
    local name="$1"
    local tag="$2"
    local url="${ACTUATOR_URL}/actuator/metrics/${name}"
    if [ -n "$tag" ]; then
        url="${url}?tag=${tag}"
    fi
    curl -s --max-time 2 "$url" 2>/dev/null \
        | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d['measurements'][0]['value'])
except Exception:
    print('')
" 2>/dev/null
}

# 진행 바 그리기 (현재값, 최대값, 너비)
draw_bar() {
    local current=$1
    local max=$2
    local width=${3:-20}
    if [ -z "$current" ] || [ -z "$max" ] || [ "$max" = "0" ]; then
        echo ""
        return
    fi
    local pct=$(python3 -c "print(min(100, int(float('$current') / float('$max') * 100)))" 2>/dev/null)
    local filled=$(python3 -c "print(min($width, int(float('$current') / float('$max') * $width)))" 2>/dev/null)
    local empty=$((width - filled))
    local color="$GREEN"
    if [ "$pct" -gt 80 ]; then
        color="$RED"
    elif [ "$pct" -gt 50 ]; then
        color="$YELLOW"
    fi
    printf "${color}"
    printf '█%.0s' $(seq 1 $filled 2>/dev/null) 2>/dev/null
    printf "${RESET}"
    printf '░%.0s' $(seq 1 $empty 2>/dev/null) 2>/dev/null
    printf " %3d%%" "$pct"
}

# 색상 표시 헬퍼 (값, warn 임계값, crit 임계값)
color_value() {
    local val=$1
    local warn=$2
    local crit=$3
    if [ -z "$val" ] || [ "$val" = "" ]; then
        printf "${RESET}-"
        return
    fi
    local cmp=$(python3 -c "print(1 if float('$val') >= float('$crit') else 0)" 2>/dev/null)
    if [ "$cmp" = "1" ]; then
        printf "${RED}${BOLD}%s${RESET}" "$val"
        return
    fi
    cmp=$(python3 -c "print(1 if float('$val') >= float('$warn') else 0)" 2>/dev/null)
    if [ "$cmp" = "1" ]; then
        printf "${YELLOW}%s${RESET}" "$val"
        return
    fi
    printf "${GREEN}%s${RESET}" "$val"
}

pct() {
    local v=$1
    if [ -z "$v" ] || [ "$v" = "" ]; then
        echo "-"
    else
        python3 -c "print(f'{float(\"$v\")*100:.1f}%')" 2>/dev/null
    fi
}

bytes_to_mb() {
    local v=$1
    if [ -z "$v" ] || [ "$v" = "" ]; then
        echo ""
    else
        python3 -c "print(round(float('$v')/1024/1024, 1))" 2>/dev/null
    fi
}

round() {
    local v=$1
    if [ -z "$v" ] || [ "$v" = "" ]; then
        echo "-"
    else
        python3 -c "v='$v'; print(int(float(v)) if v else '-')" 2>/dev/null
    fi
}

# 메인 루프
clear
trap 'echo; echo "종료됨."; exit 0' INT TERM

while true; do
    # 메트릭 수집
    cpu_process=$(fetch_metric "process.cpu.usage")
    cpu_system=$(fetch_metric "system.cpu.usage")
    heap_used=$(fetch_metric "jvm.memory.used" "area:heap")
    heap_max=$(fetch_metric "jvm.memory.max" "area:heap")
    threads=$(fetch_metric "jvm.threads.live")
    hikari_active=$(fetch_metric "hikaricp.connections.active")
    hikari_idle=$(fetch_metric "hikaricp.connections.idle")
    hikari_pending=$(fetch_metric "hikaricp.connections.pending")
    hikari_max=$(fetch_metric "hikaricp.connections.max")
    hikari_timeout=$(fetch_metric "hikaricp.connections.timeout")

    heap_used_mb=$(bytes_to_mb "$heap_used")
    heap_max_mb=$(bytes_to_mb "$heap_max")

    # 렌더링
    tput cup 0 0
    echo "┌─────────────────────────────────────────────────────────────┐"
    printf "│ ${BOLD}COUPON-API 실시간 메트릭${RESET}  @ %-30s │\n" "$ACTUATOR_URL"
    printf "│ %-59s │\n" "시간: $(date +"%H:%M:%S")     (Ctrl+C 종료, ${INTERVAL}초 주기)"
    echo "├─────────────────────────────────────────────────────────────┤"

    printf "│ ${BOLD}CPU${RESET}                                                         │\n"
    printf "│   process: %-6s  " "$(pct "$cpu_process")"
    draw_bar "$cpu_process" "1" 30
    printf " │\n"
    printf "│   system : %-6s  " "$(pct "$cpu_system")"
    draw_bar "$cpu_system" "1" 30
    printf " │\n"

    echo "├─────────────────────────────────────────────────────────────┤"
    printf "│ ${BOLD}JVM 힙${RESET}                                                      │\n"
    printf "│   used: %-6s MB / %-6s MB   " "${heap_used_mb:--}" "${heap_max_mb:--}"
    if [ -n "$heap_used_mb" ] && [ -n "$heap_max_mb" ]; then
        draw_bar "$heap_used" "$heap_max" 20
    fi
    printf " │\n"
    printf "│   threads live: %-40s │\n" "$(round "$threads")"

    echo "├─────────────────────────────────────────────────────────────┤"
    printf "│ ${BOLD}HikariCP${RESET} (DB 커넥션 풀)                                    │\n"
    printf "│   active : %-4s / %-3s   " "$(round "$hikari_active")" "$(round "$hikari_max")"
    if [ -n "$hikari_active" ] && [ -n "$hikari_max" ]; then
        draw_bar "$hikari_active" "$hikari_max" 20
    fi
    printf " │\n"
    printf "│   idle   : %-4s                                             │\n" "$(round "$hikari_idle")"
    printf "│   pending: "
    color_value "$(round "$hikari_pending")" "5" "50"
    printf "                                                │\n"
    printf "│   timeout(누적): "
    color_value "$(round "$hikari_timeout")" "1" "10"
    printf "                                             │\n"

    echo "└─────────────────────────────────────────────────────────────┘"

    # 경고 메시지
    warn=""
    if [ -n "$cpu_process" ]; then
        cpu_high=$(python3 -c "print(1 if float('$cpu_process') > 0.8 else 0)" 2>/dev/null)
        if [ "$cpu_high" = "1" ]; then
            warn="${warn}${RED}⚠  CPU 포화${RESET}  "
        fi
    fi
    if [ -n "$hikari_pending" ]; then
        pending_high=$(python3 -c "print(1 if int(float('$hikari_pending')) > 10 else 0)" 2>/dev/null)
        if [ "$pending_high" = "1" ]; then
            warn="${warn}${RED}⚠  커넥션 대기 큐${RESET}  "
        fi
    fi
    if [ -n "$hikari_timeout" ]; then
        timeout_high=$(python3 -c "print(1 if float('$hikari_timeout') > 0 else 0)" 2>/dev/null)
        if [ "$timeout_high" = "1" ]; then
            warn="${warn}${RED}⚠  커넥션 타임아웃 발생${RESET}  "
        fi
    fi

    if [ -n "$warn" ]; then
        printf "\n${BOLD}경고:${RESET} %b\n" "$warn"
    else
        printf "\n${GREEN}상태 정상${RESET}                                              \n"
    fi

    # 나머지 라인 정리
    tput ed

    sleep "$INTERVAL"
done
