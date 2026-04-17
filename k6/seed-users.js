// 유저 N명을 사전 생성하고 포인트를 충전한다.
// 부하 테스트 전에 한 번만 실행하면 된다.
//
// 실행: BASE_URL=http://localhost:8080 USERS=1000 k6 run k6/seed-users.js

import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERS = parseInt(__ENV.USERS || '1000');

export const options = {
    vus: 10,
    iterations: USERS,
};

export default function () {
    const i = __ITER + 1;
    const userRes = http.post(
        `${BASE_URL}/api/users`,
        JSON.stringify({
            email: `seed${i}@test.com`,
            name: `SeedUser${i}`,
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(userRes, { 'user created': (r) => r.status === 200 });
    const userId = userRes.json('id');

    const pointRes = http.post(
        `${BASE_URL}/api/users/${userId}/points`,
        JSON.stringify({ amount: 10000 }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(pointRes, { 'points deposited': (r) => r.status === 200 });
}

export function handleSummary(data) {
    console.log(`\n>>> Seeded ${USERS} users at ${BASE_URL}`);
    return {};
}
