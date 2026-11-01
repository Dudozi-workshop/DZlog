#!/usr/bin/env bash
# DZlog Android/Termux local-only AAB signing helper.
# No password, private key, or bundle is uploaded by this script.
set -Eeuo pipefail
export LC_ALL=C
umask 077

EXPECTED_ZIP_SHA256='d2aac0ad36b0047464223ad5d0546676c0614f4e0be9019f782db2aecc07e6d8'
EXPECTED_UPLOAD_SHA256='02:E6:59:D3:37:C4:E2:63:97:E4:0A:43:54:C7:AD:1B:C5:2A:EF:88:EF:33:26:33:00:89:7D:A3:93:9B:D8:00'
ARTIFACT_PATTERN='dzlog-v1-0-0-unsigned-DO-NOT-UPLOAD*.zip'
OUTPUT_FILENAME='DZlog-v1.0.0-vc3-signed.aab'

fail() { printf '\n[ERROR] %s\n' "$*" >&2; exit 1; }
info() { printf '\n[DZlog] %s\n' "$*"; }

# File selectors may be provided if a browser has renamed downloaded files.
artifact_override="${DZLOG_UNSIGNED_ARTIFACT:-}"
key_override="${DZLOG_UPLOAD_KEYSTORE:-}"
if [[ "$#" -gt 0 ]]; then
  if [[ "$#" -ne 2 ]]; then
    fail 'Usage: bash sign-release-aab-android.sh [artifact.zip keystore.jks]'
  fi
  artifact_override="$1"
  key_override="$2"
fi

if [[ ! -d "$HOME/storage/downloads" ]]; then
  if command -v termux-setup-storage >/dev/null 2>&1; then
    info 'Allow Termux access to Downloads when Android asks.'
    termux-setup-storage || true
  fi
fi
[[ -d "$HOME/storage/downloads" ]] || fail 'Run termux-setup-storage, grant storage access and retry.'
downloads="$HOME/storage/downloads"

if [[ -n "$artifact_override" ]]; then
  artifact="$artifact_override"
else
  mapfile -d '' candidates < <(find "$downloads" -maxdepth 1 -type f -name "$ARTIFACT_PATTERN" -print0)
  [[ "${#candidates[@]}" -eq 1 ]] || fail "Expected one release artifact ZIP in Downloads, found ${#candidates[@]}. Download run 41273005089's artifact, and remove old duplicate ZIPs."
  artifact="${candidates[0]}"
fi
[[ -f "$artifact" ]] || fail "Release ZIP not found: $artifact"

if [[ -n "$key_override" ]]; then
  key_source="$key_override"
else
  key_source="$downloads/dzlog-upload-v1.jks"
fi
[[ -f "$key_source" ]] || fail 'Upload key missing in Downloads: dzlog-upload-v1.jks (get it from your private Google Drive backup).'

info 'Verifying the GitHub artifact SHA-256 BEFORE opening the private signing key.'
if ! command -v sha256sum >/dev/null 2>&1; then
  fail 'sha256sum is missing. In Termux, run: pkg install -y coreutils'
fi
zip_hash="$(sha256sum "$artifact" | cut -d ' ' -f 1 | tr '[:upper:]' '[:lower:]')"
[[ "$zip_hash" == "$EXPECTED_ZIP_SHA256" ]] || fail "Artifact ZIP checksum mismatch. Use the exact unsigned artifact from run 41273005089. Got $zip_hash"

missing=()
for tool in keytool jarsigner unzip; do
  command -v "$tool" >/dev/null 2>&1 || missing+=("$tool")
done
if (( ${#missing[@]} > 0 )); then
  if command -v pkg >/dev/null 2>&1; then
    info 'Installing the official Termux OpenJDK and unzip tools (one-time setup).'
    pkg install -y openjdk-17 unzip || fail 'Termux installation failed. Try: pkg update -y && pkg install -y openjdk-17 unzip'
  else
    fail "Missing tools: ${missing[*]}. Use Termux and install: pkg install -y openjdk-17 unzip"
  fi
fi
for tool in keytool jarsigner unzip; do
  command -v "$tool" >/dev/null 2>&1 || fail "Still missing $tool after setup."
done

out="$downloads/$OUTPUT_FILENAME"
[[ ! -e "$out" ]] || fail "Signed output already exists. Move it aside before a new attempt: $out"
workdir="$(mktemp -d "${TMPDIR:-$HOME}/dzlog-sign.XXXXXXXX")"
trap 'unset DZLOG_UPLOAD_KEY_PASSWORD 2>/dev/null || true; rm -rf -- "$workdir"' EXIT

info 'Preparing an isolated copy of the keystore and unsigned App Bundle.'
cp -- "$key_source" "$workdir/upload.jks"
entry="$(unzip -Z -1 "$artifact" | grep -E '(^|/)app-release\.aab$' | head -n 1 || true)"
[[ -n "$entry" ]] || fail 'GitHub artifact ZIP does not contain app-release.aab.'
unzip -p "$artifact" "$entry" > "$workdir/unsigned.aab"
[[ -s "$workdir/unsigned.aab" ]] || fail 'Extracted AAB is empty.'
unzip -Z -1 "$workdir/unsigned.aab" | grep -Fxq 'base/manifest/AndroidManifest.xml' || fail 'The unsigned file is not a recognizable Android App Bundle.'

info 'Checking the private upload key locally.'
printf 'Enter the UPLOAD KEY password here (hidden, never sent): ' >&2
IFS= read -r -s DZLOG_UPLOAD_KEY_PASSWORD || fail 'Could not read password from the local terminal.'
printf '\n' >&2
[[ -n "$DZLOG_UPLOAD_KEY_PASSWORD" ]] || fail 'The keystore password cannot be empty.'
export DZLOG_UPLOAD_KEY_PASSWORD
keytool -exportcert -alias upload -storetype PKCS12 -storepass:env DZLOG_UPLOAD_KEY_PASSWORD -keystore "$workdir/upload.jks" -file "$workdir/certificate.der" || fail 'Could not open keystore/alias upload.'
cert_fingerprint="$(keytool -printcert -file "$workdir/certificate.der" | grep -m 1 'SHA256:' | sed -E 's/.*SHA256:[[:space:]]*//' | tr '[:lower:]' '[:upper:]' | tr -d '[:space:]')"
[[ "$cert_fingerprint" == "$EXPECTED_UPLOAD_SHA256" ]] || fail 'Local upload key certificate differs from the approved Play upload key. Signing STOPPED.'
info 'Upload certificate SHA-256 verified. Signing AAB locally with alias upload.'
jarsigner -keystore "$workdir/upload.jks" -storetype PKCS12 -storepass:env DZLOG_UPLOAD_KEY_PASSWORD -keypass:env DZLOG_UPLOAD_KEY_PASSWORD -digestalg SHA-256 -sigalg SHA256withRSA -signedjar "$workdir/signed.aab" "$workdir/unsigned.aab" upload || fail 'jarsigner failed. Check the local upload key password.'
unset DZLOG_UPLOAD_KEY_PASSWORD
[[ -s "$workdir/signed.aab" ]] || fail 'Signed output was not created.'

info 'Verifying AAB signature and the signer certificate fingerprint.'
verify_log="$workdir/verify.log"
jarsigner -verify -verbose -certs "$workdir/signed.aab" > "$verify_log" 2>&1 || fail 'jarsigner signature verification failed.'
grep -Fq 'jar verified.' "$verify_log" || fail 'jarsigner did not report jar verified.'
unzip -Z -1 "$workdir/signed.aab" | grep -Eq '^META-INF/[^/]+\.(RSA|EC|DSA)$' || fail 'No AAB signature block was found.'
signed_fingerprint="$(keytool -printcert -jarfile "$workdir/signed.aab" | grep -m 1 'SHA256:' | sed -E 's/.*SHA256:[[:space:]]*//' | tr '[:lower:]' '[:upper:]' | tr -d '[:space:]')"
[[ "$signed_fingerprint" == "$EXPECTED_UPLOAD_SHA256" ]] || fail 'Signed AAB signer certificate does not match expected upload key.'

cp -- "$workdir/signed.aab" "$out"
info 'SUCCESS: Play-upload-key-signed AAB was created and verified ON THIS PHONE.'
printf 'Signed file: %s\n' "$out"
printf 'Signed AAB SHA-256: %s\n' "$(sha256sum "$out" | cut -d ' ' -f 1)"
printf '\nNEXT: Upload only the signed AAB into Google Play INTERNAL TESTING.\n'
printf 'Security: remove the temporary keystore copy from Downloads after signing; keep private backups safe.\n'
