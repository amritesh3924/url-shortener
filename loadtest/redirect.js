import http from 'k6/http';
import { check } from 'k6';
import { sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
export const options = {
  setupTimeout: '6m',
  scenarios: { redirects: { executor: 'constant-vus', vus: 50, duration: '30s' } },
  thresholds: { http_req_duration: ['p(95)<2000'] },
};

export function setup() {
  const codes = [];
  for (let index = 0; index < 100; index++) {
    const response = http.post(`${baseUrl}/api/shorten`, JSON.stringify({ longUrl: `https://example.com/load-test/${index}` }), {
      headers: { 'Content-Type': 'application/json' },
    });
    check(response, { 'link created': (result) => result.status === 200 });
    if (response.status === 200) codes.push(response.json('shortCode'));
    // The API limit is 20 links per minute from one IP, including this setup.
    if ((index + 1) % 20 === 0 && index < 99) sleep(60);
  }
  if (codes.length === 0) throw new Error('No links were created');
  return { codes };
}

export default function (data) {
  const code = data.codes[Math.floor(Math.random() * data.codes.length)];
  const response = http.get(`${baseUrl}/${code}`, { redirects: 0 });
  check(response, { 'redirect returned': (result) => result.status === 302 });
}

export function handleSummary(data) {
  const requestsPerSecond = data.metrics.http_reqs.values.rate;
  const p95 = data.metrics.http_req_duration.values['p(95)'];
  console.log(`Requests/sec: ${requestsPerSecond.toFixed(2)}`);
  console.log(`p95 latency: ${p95.toFixed(2)} ms`);
  return {};
}
