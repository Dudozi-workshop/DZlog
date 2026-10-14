# DZlog Pro — Dormant purchase verification service

## Current shipping behavior
- **ALL_UNLOCKED** by default. No billing connection, Pro purchase button, limit, or paywall.
- Existing 5+5 filename/path slots and saved templates stay fully accessible.
- This directory prepares the **server side** of a future one-time purchase.
- Nothing here is deployed or activated by committing it to GitHub.

## Product contract
- Android application ID: `com.dudoziworkshop.dzlog`
- Non-consumable Play product ID: `dzlog_pro_lifetime`
- Planned KRW base price: **5,900** (must configure/confirm in Google Play Console; never show an unverified hardcoded price).
- Proposed PAID_RELEASE after approval: Free 3+3 slots / verified Pro 5+5; Free saved-template quota **N is still unapproved**.
- Server-issued grant: RSA SHA256 JWT, `iss=dzlog-billing`, `aud=com.dudoziworkshop.dzlog`, `sku=dzlog_pro_lifetime`, `iat`, `exp=iat+30 days`, SHA256 purchase-token fingerprint `sub`. Android validates signature and expiry locally.
- Verified purchase source: Google Play Developer API `purchases.products.get` with server-only service account, matching fixed package/SKU and PURCHASED/unconsumed state. PENDING/unowned/voided orders cannot mint grants.
- On confirmed refunded/voided state, the app deletes the cached proof. **Offline devices cannot receive a refund event until reconnection**; the most a cached proof can remain valid is 30 days.

## Test without credentials
```sh
node --test billing-server/test/*.test.mjs
```
Uses Node 20+ standard modules. CI also runs this and Android `testDebugUnitTest`/ `assembleDebug`.

## Operator setup — required ONLY before activating payments
1. Google Play Console: create & activate the one-time **non-consumable** product `dzlog_pro_lifetime`, set Korean price to 5,900 KRW, add license testers. Link a Google Cloud service account with appropriate Play Android Publisher API access. This MUST be completed outside this repository.
2. Generate a **separate** RSA 2048+ signing keypair for 30-day grants (do **not** reuse the Google service account credential for signing):
   ```sh
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out dzlog-entitlement-private.pem
   openssl pkey -in dzlog-entitlement-private.pem -pubout -out dzlog-entitlement-public.pem
   ```
   Keep both service-account JSON and the grant-signing private PEM in a managed secrets system; **never commit** them.
3. Configure a HTTPS reverse proxy/security gateway with per-client rate limiting, access logging that **never logs purchase tokens**, and secured environment variables:
   - `DZLOG_GOOGLE_SERVICE_ACCOUNT_JSON` — whole service-account JSON
   - `DZLOG_ENTITLEMENT_SIGNING_KEY_PEM` — private key PEM (may encode newlines as literal `\\n`)
   - `HOST=127.0.0.1`, `PORT=8080`
   - Start `node billing-server/src/server.mjs`.
   - `GET /health` is a non-sensitive readiness probe.
   - `POST /v1/verify` with `{"purchaseToken":"..."}` returns `{"state":"VERIFIED","grant":"<signed JWT>"}`, `{"state":"NOT_OWNED"}` or HTTP 503 `{"state":"UNKNOWN"}` on API outage.
   - The reverse proxy must serve HTTPS: Android **rejects HTTP** and has no credential or private signing key embedded.
4. On Android paid build, supply securely via Gradle properties:
   ```properties
   dzlogBillingVerifierUrl=https://YOUR_VERIFIER_HOST
   dzlogBillingPublicKeyB64=BASE64_OF_PUBLIC_PEM
   ```
   Both are **public configuration** only; do not put Play service account JSON/secret keys in the Android APK. Empty default values fail closed (cannot purchase).
5. Confirm exact **Free saved-template limit N and Pro quota policy** with the product owner. Set values in `MonetizationRolloutConfig`; do not assume the sample test's 3-item quota is approved.
6. Validate all automated tests, sandbox/test purchases, Pending, restore, cancel, refund/revoke, 29/30/31-day offline expiry, clock rollback, and 5-slot capture. Test both a new and upgraded existing user, including over-quota template backups and preserving preexisting media.
7. **Only after the above**, build a new reviewed release with `MonetizationRolloutConfig.mode = PAID_RELEASE`; change of this compile-time constant requires a store update. Existing app versions remain `ALL_UNLOCKED`; retroactive one-tap remote gating is **not** implemented.
8. Before Play release, separately update/QA app `targetSdk` to meet the then-current Play deadline (the compile SDK change alone is not enough), with camera/storage permission and Android gesture navigation regressions tested.

## Remaining integrations / honest constraints
- Production Google Play Console product/service account credentials, HTTPS backend hosting, app signing, real payment test devices, and RTDN/Voided Purchases monitoring need operator access.
- A valid signed offline proof is cryptographically protected; however a customer changing an offline device clock can affect wall-clock expiration checks. Code rejects clock rollback relative to the last observed time, and on-network ownership must be revalidated when connection returns. For a stronger production threat model, use a trusted monotonic/server checkpoint.
- The code is **activation-ready scaffold**, not proof of a live production financial checkout until real purchase, refund, reconnect and release QA is passed.
