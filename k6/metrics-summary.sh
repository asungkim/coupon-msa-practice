#!/bin/bash
# metrics-collector.sh 결과 CSV를 분석해 peak 값을 markdown으로 출력
#
# 사용법:
#   ./k6/metrics-summary.sh <CSV파일>

set -e

CSV="$1"
if [ -z "$CSV" ] || [ ! -f "$CSV" ]; then
    echo "사용법: $0 <CSV파일>"
    exit 1
fi

python3 - << PYEOF
import csv

rows = []
with open("$CSV") as f:
    reader = csv.DictReader(f)
    for r in reader:
        rows.append(r)

def to_float(v):
    try:
        return float(v) if v and v != '' else None
    except:
        return None

def peak(col):
    vals = [to_float(r[col]) for r in rows]
    vals = [v for v in vals if v is not None]
    return max(vals) if vals else None

def avg_v(col):
    vals = [to_float(r[col]) for r in rows]
    vals = [v for v in vals if v is not None]
    return sum(vals) / len(vals) if vals else None

def fmt_pct(v):
    return f"{v*100:.1f}%" if v is not None else "-"

def fmt(v, digits=1):
    return f"{v:.{digits}f}" if v is not None else "-"

def fmt_int(v):
    return f"{int(v)}" if v is not None else "-"

print("**서버 메트릭 (peak / avg)**\n")
print("| 메트릭 | peak | avg |")
print("|-------|------|-----|")
print(f"| CPU process | {fmt_pct(peak('cpu_process'))} | {fmt_pct(avg_v('cpu_process'))} |")
print(f"| CPU system | {fmt_pct(peak('cpu_system'))} | {fmt_pct(avg_v('cpu_system'))} |")
print(f"| heap used (MB) | {fmt(peak('heap_used_mb'))} | {fmt(avg_v('heap_used_mb'))} |")
print(f"| heap max (MB) | {fmt(peak('heap_max_mb'))} | - |")
print(f"| threads live | {fmt_int(peak('threads_live'))} | {fmt(avg_v('threads_live'))} |")
print(f"| hikari active | {fmt_int(peak('hikari_active'))} | {fmt(avg_v('hikari_active'))} |")
print(f"| hikari pending | {fmt_int(peak('hikari_pending'))} | {fmt(avg_v('hikari_pending'))} |")
print(f"| hikari idle | {fmt_int(peak('hikari_idle'))} | {fmt(avg_v('hikari_idle'))} |")
print(f"| hikari timeout (total) | {fmt_int(peak('hikari_timeout_total'))} | - |")
PYEOF
