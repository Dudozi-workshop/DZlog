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

# Diagnostics only: unsigned .app is NOT installable on a physical iPhone.
printf '%s\n' 'Unsigned iPhone device build (arm64-apple-ios); requires Apple provisioning and signing before installation.' > "$evidence_dir/README-UNSIGNED.txt"
ditto -c -k --keepParent "$app_dir" "$evidence_dir/dzlog-ios-device-UNSIGNED.zip"
shasum -a 256 "$evidence_dir/dzlog-ios-device-UNSIGNED.zip" > "$evidence_dir/sha256.txt"
