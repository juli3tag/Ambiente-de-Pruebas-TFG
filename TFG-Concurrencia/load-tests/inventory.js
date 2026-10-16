import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const successCounter = new Counter('app_success');
const conflictCounter = new Counter('app_conflict');
const outOfStockCounter = new Counter('app_out_of_stock');
const errorCounter = new Counter('app_error');

const targetVus = __ENV.VUS ? parseInt(__ENV.VUS) : 100;

export const options = {
    stages: [
        { duration: '10s', target: targetVus },
        { duration: '60s', target: targetVus },
        { duration: '5s', target: 0 },
    ],
};

function getProductId() {

    const isHot = Math.random() < 0.8;

    if (isHot) {
        return Math.floor(Math.random() * 2000) + 1;
    }

    return Math.floor(Math.random() * 8000) + 2001;
}

export default function () {

    const strategy = __ENV.STRATEGY
        ? __ENV.STRATEGY.toLowerCase()
        : 'occ';

    const url = `http://localhost:8080/inventory/purchase/${strategy}`;

    const payload = JSON.stringify({
        productId: getProductId(),
        quantity: 1
    });

    const params = {
        headers: {
            'Content-Type': 'application/json'
        }
    };

    const res = http.post(url, payload, params);

    if (res.status === 200) {

        successCounter.add(1);

    } else if (res.status === 409) {

        conflictCounter.add(1);

    } else if (res.status === 400) {

        outOfStockCounter.add(1);

    } else {

        errorCounter.add(1);
    }

    check(res, {
        'request handled': (r) =>
            [200, 400, 409, 500].includes(r.status)
    });
}