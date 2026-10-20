#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
evidence_dir="$repo_root/build/ios-poc/evidence"
mkdir -p "$evidence_dir"
device_id="$(xcrun simctl list devices available -j | python3 -c 'import json,sys; d=json.load(sys.stdin); print(next(x["udid"] for xs in d["devices"].values() for x in xs if x.get("isAvailable") and x["name"].startswith("iPhone")))')"
trap 'xcrun simctl shutdown "$device_id" || true' EXIT
xcrun simctl boot "$device_id" || true
xcrun simctl bootstatus "$device_id" -b
xcrun simctl status_bar "$device_id" override --time '9:41' --batteryState charged --batteryLevel 100
xcode_args=(
  -project "$repo_root/iosApp/DZlogUiPoc.xcodeproj" -scheme DZlogUiPoc
  -destination "platform=iOS Simulator,id=$device_id"
  -derivedDataPath "$repo_root/build/ios-poc/DerivedData"
  -parallel-testing-enabled NO CODE_SIGNING_ALLOWED=NO
)
# Build once, then verify the platform bundle before launching the UI tests.
xcodebuild build-for-testing "${xcode_args[@]}" | tee "$evidence_dir/host-build.log"
bundled_png="$repo_root/build/ios-poc/DerivedData/Build/Products/Debug-iphonesimulator/DZlogUiPoc.app/home_leaf_shadow_c.png"
cmp "$repo_root/iosApp/PoCApp/home_leaf_shadow_c.png" "$bundled_png"
shasum -a 256 "$bundled_png" | tee "$evidence_dir/home-leaf-bundle-sha256.txt"
xcodebuild test-without-building "${xcode_args[@]}" \
  -resultBundlePath "$evidence_dir/interaction.xcresult" \
  | tee "$evidence_dir/interaction-build.log"
