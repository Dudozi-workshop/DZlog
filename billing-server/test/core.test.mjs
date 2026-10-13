import test from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPairSync, createVerify } from 'node:crypto';
import {
  PACKAGE_NAME, PRODUCT_ID, classifyGooglePurchase,
  signEntitlement, verifyAndGrant, tokenHash,
} from '../src/core.mjs';

const { privateKey, publicKey } = generateKeyPairSync('rsa', {
  modulusLength: 2048,
});
const token = 'PLAY-PURCHASE-TOKEN-EXAMPLE-DO-NOT-USE';

test('Play purchase statuses are validated on server', () => {
  assert.equal(classifyGooglePurchase({ productId: PRODUCT_ID, purchaseState: 0,
    consumptionState: 0 }), 'VERIFIED');
  for (const entry of [
    { productId: PRODUCT_ID, purchaseState: 1, consumptionState: 0 },
    { productId: 'wrong', purchaseState: 0, consumptionState: 0 },
    { productId: PRODUCT_ID, purchaseState: 0, consumptionState: 1 },
  ]) assert.equal(classifyGooglePurchase(entry), 'NOT_OWNED');
  assert.equal(classifyGooglePurchase({ productId: PRODUCT_ID, purchaseState: 0,
    consumptionState: 0, refundableQuantity: 0 }), 'REVOKED');
});

test('signed entitlement has 30-day expiry, correct package and SKU', () => {
  const proof = signEntitlement({
    purchaseToken: token,
    nowSeconds: 1_790_000_000,
    privateKey,
  });
  const [header, payload, signature] = proof.split('.');
  const decoded = JSON.parse(Buffer.from(payload, 'base64url').toString());
  assert.equal(decoded.aud, PACKAGE_NAME);
  assert.equal(decoded.sku, PRODUCT_ID);
  assert.equal(decoded.sub, tokenHash(token));
  assert.equal(decoded.exp - decoded.iat, 30 * 24 * 3600);
  assert.equal(JSON.parse(Buffer.from(header, 'base64url').toString()).alg, 'RS256');
  assert.equal(createVerify('RSA-SHA256').update(header + '.' + payload)
    .verify(publicKey, Buffer.from(signature, 'base64url')), true);
});

test('purchase verification only issues certificate after Google confirms ownership', async () => {
  const serviceAccount = {
    client_email: 'fake-account@example.iam.gserviceaccount.com',
    private_key: privateKey.export({ type: 'pkcs8', format: 'pem' }),
  };
  let googleState = 0;
  const calls = [];
  const mockFetch = async (url) => {
    calls.push(url);
    if (String(url).startsWith('https://oauth2.googleapis.com')) {
      return { ok: true, json: async () => ({ access_token: 'TEST_ONLY' }) };
    }
    return { ok: true, json: async () => ({
      productId: PRODUCT_ID, purchaseState: googleState, consumptionState: 0,
    }) };
  };
  let result = await verifyAndGrant({
    token, serviceAccount, entitlementPrivateKey: privateKey,
    nowSeconds: 1_790_000_000, fetchImpl: mockFetch,
  });
  assert.equal(result.state, 'VERIFIED');
  assert.ok(result.grant);
  assert.ok(calls[1].includes(encodeURIComponent(token)));
  googleState = 1;
  result = await verifyAndGrant({
    token, serviceAccount, entitlementPrivateKey: privateKey,
    nowSeconds: 1_790_000_001, fetchImpl: mockFetch,
  });
  assert.equal(result.state, 'NOT_OWNED');
  assert.equal(result.grant, undefined);
});
