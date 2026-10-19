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
xcodebuild test \
  -project "$repo_root/iosApp/DZlogUiPoc.xcodeproj" -scheme DZlogUiPoc \
  -destination "platform=iOS Simulator,id=$device_id" \
  -derivedDataPath "$repo_root/build/ios-poc/DerivedData" \
  -resultBundlePath "$evidence_dir/interaction.xcresult" \
  -parallel-testing-enabled NO CODE_SIGNING_ALLOWED=NO \
  | tee "$evidence_dir/interaction-build.log"
