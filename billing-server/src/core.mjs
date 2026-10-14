import { createHash, createPrivateKey, createSign, sign as cryptoSign } from 'node:crypto';

export const PACKAGE_NAME = 'com.dudoziworkshop.dzlog';
export const PRODUCT_ID = 'dzlog_pro_lifetime';
const GRACE_SECONDS = 30 * 24 * 60 * 60;

const b64url = input => Buffer.from(input).toString('base64url');
export const tokenHash = token => createHash('sha256').update(token).digest('hex');

export function classifyGooglePurchase(data) {
  if (!data || data.productId !== PRODUCT_ID) return 'NOT_OWNED';
  if (data.purchaseState !== 0 || data.consumptionState !== 0) return 'NOT_OWNED';
  if (data.refundableQuantity !== undefined && data.refundableQuantity < 1) return 'REVOKED';
  return 'VERIFIED';
}

export function signEntitlement({ purchaseToken, nowSeconds, privateKey }) {
  if (!purchaseToken || !privateKey || !Number.isInteger(nowSeconds)) {
    throw Error('Invalid entitlement signing inputs');
  }
  const header = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const payload = b64url(JSON.stringify({
    iss: 'dzlog-billing',
    aud: PACKAGE_NAME,
    sub: tokenHash(purchaseToken),
    sku: PRODUCT_ID,
    iat: nowSeconds,
    exp: nowSeconds + GRACE_SECONDS,
  }));
  const unsigned = header + '.' + payload;
  const signature = cryptoSign('RSA-SHA256', Buffer.from(unsigned), privateKey)
    .toString('base64url');
  return unsigned + '.' + signature;
}

/** OAuth2 service-account JWT, used only within server infrastructure. */
export function googleAccessAssertion({ serviceAccount, nowSeconds }) {
  const header = b64url(JSON.stringify({ typ: 'JWT', alg: 'RS256' }));
  const payload = b64url(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: 'https://www.googleapis.com/auth/androidpublisher',
    aud: 'https://oauth2.googleapis.com/token',
    iat: nowSeconds,
    exp: nowSeconds + 3600,
  }));
  const unsigned = header + '.' + payload;
  const signature = cryptoSign('RSA-SHA256', Buffer.from(unsigned),
    createPrivateKey(serviceAccount.private_key)).toString('base64url');
  return unsigned + '.' + signature;
}

export async function getAccessToken(serviceAccount, fetchImpl = fetch) {
  const nowSeconds = Math.floor(Date.now() / 1000);
  const body = new URLSearchParams({
    grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
    assertion: googleAccessAssertion({ serviceAccount, nowSeconds }),
  });
  const response = await fetchImpl('https://oauth2.googleapis.com/token', {
    method: 'POST', headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body, signal: AbortSignal.timeout(10000),
  });
  if (!response.ok) throw Error('Google OAuth unavailable');
  const data = await response.json();
  if (typeof data.access_token !== 'string') throw Error('Google OAuth invalid');
  return data.access_token;
}

export async function verifyPlayProduct({ purchaseToken, accessToken, fetchImpl = fetch }) {
  const url = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/' +
    encodeURIComponent(PACKAGE_NAME) + '/purchases/products/' +
    encodeURIComponent(PRODUCT_ID) + '/tokens/' + encodeURIComponent(purchaseToken);
  const response = await fetchImpl(url, {
    method: 'GET', headers: { authorization: 'Bearer ' + accessToken },
    signal: AbortSignal.timeout(10000),
  });
  // Token known to be invalid is distinct from a transient backend outage.
  if (response.status === 404 || response.status === 410) return { state: 'NOT_OWNED' };
  if (!response.ok) throw Error('Google Play unavailable');
  return { state: classifyGooglePurchase(await response.json()) };
}

/**
 * No user-provided SKU/package/price accepted. Product identity is server-side fixed.
 * Google and server credentials are never sent to the Android client.
 */
export async function verifyAndGrant({ token, serviceAccount, entitlementPrivateKey,
  fetchImpl = fetch, nowSeconds = Math.floor(Date.now() / 1000) }) {
  if (typeof token !== 'string' || token.length < 15 || token.length > 4096)
    return { state: 'NOT_OWNED' };
  const accessToken = await getAccessToken(serviceAccount, fetchImpl);
  const purchase = await verifyPlayProduct({ purchaseToken: token, accessToken, fetchImpl });
  if (purchase.state !== 'VERIFIED') return { state: purchase.state };
  return {
    state: 'VERIFIED',
    grant: signEntitlement({
      purchaseToken: token, nowSeconds, privateKey: entitlementPrivateKey,
    }),
  };
}
