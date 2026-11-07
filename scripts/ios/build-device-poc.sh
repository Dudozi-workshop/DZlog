#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
framework_dir="$repo_root/sharedUi/build/bin/iosArm64/debugFramework"
out_dir="$repo_root/build/ios-device-poc"
app_dir="$out_dir/DZlogUiPoc.app"
evidence_dir="$out_dir/evidence"

# The iOS device framework must be built separately from the simulator binary.
test -d "$framework_dir/DZlogUi.framework" || {
  echo "Missing iosArm64 DZlogUi.framework. Build :sharedUi:linkDebugFrameworkIosArm64 first." >&2
  exit 1
}
mkdir -p "$app_dir/Frameworks" "$evidence_dir"

xcrun --sdk iphoneos swiftc -parse-as-library \
  -sdk "$(xcrun --sdk iphoneos --show-sdk-path)" \
  -target arm64-apple-ios15.0 \
  -F "$framework_dir" -framework DZlogUi \
  -Xlinker -rpath -Xlinker @executable_path/Frameworks \
  "$repo_root/iosApp/PoCApp/AppDelegate.swift" -o "$app_dir/DZlogUiPoc"

cp "$repo_root/iosApp/PoCApp/Info.plist" "$app_dir/Info.plist"
cp "$repo_root/iosApp/PoCApp/home_leaf_shadow_c.png" "$app_dir/home_leaf_shadow_c.png"
ditto "$framework_dir/DZlogUi.framework" "$app_dir/Frameworks/DZlogUi.framework"

# arm64 alone does not distinguish an iPhone binary from an arm64 simulator binary.
plutil -lint "$app_dir/Info.plist" | tee "$evidence_dir/plist-check.txt"
xcrun vtool -show-build "$app_dir/DZlogUiPoc" | tee "$evidence_dir/app-platform.txt"
xcrun vtool -show-build "$app_dir/Frameworks/DZlogUi.framework/DZlogUi" | tee "$evidence_dir/framework-platform.txt"
grep -Eq '^[[:space:]]*platform IOS[[:space:]]*$' "$evidence_dir/app-platform.txt"
grep -Eq '^[[:space:]]*platform IOS[[:space:]]*$' "$evidence_dir/framework-platform.txt"

# Preserve the original unsigned device build for compile diagnostics.
printf '%s\n' 'Unsigned iPhone device build (arm64-apple-ios); requires Apple provisioning and signing before direct installation.' > "$evidence_dir/README-UNSIGNED.txt"
ditto -c -k --keepParent "$app_dir" "$evidence_dir/dzlog-ios-device-UNSIGNED.zip"
shasum -a 256 "$evidence_dir/dzlog-ios-device-UNSIGNED.zip" > "$evidence_dir/sha256.txt"

# Public cloud device farms replace the signature and provisioning profile.
# Package a structurally valid device-target IPA without Apple credentials.
# This artifact is for a re-signing service only and is not directly installable.
cloud_root="$out_dir/cloud-resign"
cloud_app="$cloud_root/Payload/DZlogUiPoc.app"
rm -rf "$cloud_root"
mkdir -p "$cloud_root/Payload"
ditto "$app_dir" "$cloud_app"
codesign --force --sign - --timestamp=none "$cloud_app/Frameworks/DZlogUi.framework"
codesign --force --sign - --timestamp=none "$cloud_app"
codesign --verify --deep --strict "$cloud_app"
(
  cd "$cloud_root"
  zip -qry "$evidence_dir/dzlog-ios-device-CLOUD-RESIGN.ipa" Payload
)
printf '%s\n' 'Device-target IPA for AWS Device Farm or BrowserStack automatic re-signing. Not directly installable and contains no Apple credentials.' > "$evidence_dir/README-CLOUD-RESIGN.txt"
shasum -a 256 "$evidence_dir/dzlog-ios-device-CLOUD-RESIGN.ipa" >> "$evidence_dir/sha256.txt"
