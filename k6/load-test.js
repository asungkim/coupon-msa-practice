// 쿠폰 발급 부하 테스트 (pre-seed된 유저 + 외부 생성 쿠폰 사용).
//
// 실행은 run-test.sh 래퍼로 한다:
//   ./k6/run-test.sh "라벨" http://localhost:9000 1000 200
//
// 환경변수:
//   BASE_URL   : 대상 서버
//   VUS        : 동시 Virtual User 수
//   QUANTITY   : 쿠폰 총 수량
//   COUPON_ID  : 외부에서 생성한 쿠폰 id
//   LABEL      : 결과 파일에 기록할 라벨

import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9000';
const VUS = parseInt(__ENV.VUS || '300');
const QUANTITY = parseInt(__ENV.QUANTITY || '200');
const COUPON_ID = parseInt(__ENV.COUPON_ID || '1');
const LABEL = __ENV.LABEL || 'unknown';

export const options = {
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    scenarios: {
        burst: {
            executor: 'per-vu-iterations',
            vus: VUS,
            iterations: 1,
            maxDuration: '120s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<1.0'],
    },
};

export default function () {
    const userId = __VU;
    const res = http.post(
        `${BASE_URL}/api/coupons/${COUPON_ID}/issue`,
        JSON.stringify({ userId }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, {
        '200 success': (r) => r.status === 200,
    });
}

export function handleSummary(data) {
    // DB 정합성 조회
    const coupon = http.get(`${BASE_URL}/api/coupons/${COUPON_ID}`).json();
    const issueCount = parseInt(
        http.get(`${BASE_URL}/api/coupons/${COUPON_ID}/issues/count`).body
    );
    const consumed = coupon.totalQuantity - coupon.remainingQuantity;
    const consistent = consumed === issueCount;

    // HTTP 메트릭
    const m = data.metrics;
    const get = (k) => (m[k] ? m[k].values : {});
    const reqs = get('http_reqs');
    const dur = get('http_req_duration');
    const wait = get('http_req_waiting');
    const failed = get('http_req_failed');
    const iter = get('iterations');
    const iterDur = get('iteration_duration');
    const dataRecv = get('data_received');
    const dataSent = get('data_sent');

    const checks200 = (data.root_group && data.root_group.checks || [])
        .find((c) => c.name === '200 success');
    const successCount = checks200 ? checks200.passes : 0;
    const failCount = checks200 ? checks200.fails : 0;

    const fmt = (v, d = 2) => (v == null ? '-' : Number(v).toFixed(d));
    const fmtMs = (v) => (v == null ? '-' : `${Number(v).toFixed(1)} ms`);
    const fmtKB = (v) => (v == null ? '-' : `${(Number(v) / 1024).toFixed(1)} KB`);

    const ts = new Date().toISOString().replace('T', ' ').substring(0, 16);

    const md =
        `\n## [${ts}] ${LABEL}\n\n` +
        `| 항목 | 값 |\n|------|-----|\n` +
        `| BASE_URL | ${BASE_URL} |\n` +
        `| 동시 VU | ${VUS} |\n` +
        `| 쿠폰 수량 | ${QUANTITY} |\n` +
        `| **총 요청 수** | ${reqs.count ?? '-'} |\n` +
        `| **TPS (req/s)** | ${fmt(reqs.rate)} |\n` +
        `| iterations | ${iter.count ?? '-'} |\n` +
        `| iteration_duration avg | ${fmtMs(iterDur.avg)} |\n` +
        `| **http_req_duration avg** | ${fmtMs(dur.avg)} |\n` +
        `| http_req_duration min | ${fmtMs(dur.min)} |\n` +
        `| http_req_duration p50 | ${fmtMs(dur.med)} |\n` +
        `| http_req_duration p90 | ${fmtMs(dur['p(90)'])} |\n` +
        `| **http_req_duration p95** | ${fmtMs(dur['p(95)'])} |\n` +
        `| **http_req_duration p99** | ${fmtMs(dur['p(99)'])} |\n` +
        `| http_req_duration max | ${fmtMs(dur.max)} |\n` +
        `| http_req_waiting avg (TTFB) | ${fmtMs(wait.avg)} |\n` +
        `| **http_req_failed rate** | ${(Number(failed.rate || 0) * 100).toFixed(2)}% |\n` +
        `| 200 성공 | ${successCount} |\n` +
        `| 200 실패 | ${failCount} |\n` +
        `| totalQuantity | ${coupon.totalQuantity} |\n` +
        `| remainingQuantity | ${coupon.remainingQuantity} |\n` +
        `| 실제 발급(DB) | ${issueCount} |\n` +
        `| 소비량(total-remaining) | ${consumed} |\n` +
        `| **정합성** | ${consistent ? 'PASS ✅' : 'FAIL ❌'} |\n` +
        `| data_received | ${fmtKB(dataRecv.count)} |\n` +
        `| data_sent | ${fmtKB(dataSent.count)} |\n`;

    const textOut =
        `\n========== [${LABEL}] ==========\n` +
        `Requests: ${reqs.count ?? '-'} | TPS: ${fmt(reqs.rate)} req/s\n` +
        `Latency avg/p95/p99: ${fmtMs(dur.avg)} / ${fmtMs(dur['p(95)'])} / ${fmtMs(dur['p(99)'])}\n` +
        `200 success/fail: ${successCount} / ${failCount}\n` +
        `DB consistency: ${consistent ? 'PASS' : 'FAIL'} (consumed=${consumed}, issueCount=${issueCount})\n` +
        `=================================\n`;

    return {
        'stdout': textOut,
        'k6/last-run.md': md,
    };
}
