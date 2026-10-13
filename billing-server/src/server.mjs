import http from 'node:http';
import { verifyAndGrant } from './core.mjs';

function readJson(request) {
  return new Promise((resolve, reject) => {
    let bytes = 0;
    let text = '';
    request.on('data', chunk => {
      bytes += chunk.length;
      if (bytes > 8192) {
        reject(Error('Request too large'));
        request.destroy();
        return;
      }
      text += chunk;
    });
    request.on('end', () => {
      try { resolve(JSON.parse(text)); } catch { reject(Error('Invalid JSON')); }
    });
    request.on('error', reject);
  });
}

function responseJson(response, status, data) {
  response.writeHead(status, {
    'Content-Type': 'application/json',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
  });
  response.end(JSON.stringify(data));
}

export function makeHandler(options) {
  const { serviceAccount, entitlementPrivateKey, fetchImpl = fetch } = options;
  return async (request, response) => {
    if (request.url === '/health' && request.method === 'GET') {
      responseJson(response, 200, { status: 'ok' });
      return;
    }
    if (request.url !== '/v1/verify' || request.method !== 'POST') {
      responseJson(response, 404, { error: 'not_found' });
      return;
    }
    if (request.headers['content-type']?.split(';')[0] !== 'application/json') {
      responseJson(response, 415, { error: 'unsupported_media_type' });
      return;
    }
    try {
      const { purchaseToken } = await readJson(request);
      const result = await verifyAndGrant({
        token: purchaseToken, serviceAccount, entitlementPrivateKey, fetchImpl,
      });
      responseJson(response, 200, result);
    } catch {
      // Never mark an unavailable Google API as a revocation; the client can use
      // an already verified, unexpired 30-day offline grant during this outage.
      responseJson(response, 503, { state: 'UNKNOWN' });
    }
  };
}

if (process.argv[1] && import.meta.url === new URL('file://' + process.argv[1]).href) {
  if (!process.env.DZLOG_GOOGLE_SERVICE_ACCOUNT_JSON ||
      !process.env.DZLOG_ENTITLEMENT_SIGNING_KEY_PEM) {
    throw Error('Billing verification credentials are missing; refusing to serve');
  }
  const serviceAccount = JSON.parse(process.env.DZLOG_GOOGLE_SERVICE_ACCOUNT_JSON);
  const signingKey = process.env.DZLOG_ENTITLEMENT_SIGNING_KEY_PEM.replace(/\\n/g, '\n');
  const port = Number(process.env.PORT || 8080);
  const host = process.env.HOST || '127.0.0.1';
  http.createServer(makeHandler({
    serviceAccount, entitlementPrivateKey: signingKey,
  })).listen(port, host, () => console.info('DZlog billing verifier listening'));
}
