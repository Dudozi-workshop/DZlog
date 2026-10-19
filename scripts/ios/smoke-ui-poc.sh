#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
evidence_dir="$repo_root/build/ios-poc/evidence"
mkdir -p "$evidence_dir"
device_id="$(xcrun simctl list devices available -j | python3 -c 'import json,sys; d=json.load(sys.stdin); print(next(x["udid"] for xs in d["devices"].values() for x in xs if x.get("isAvailable") and x["name"].startswith("iPhone")))')"
# Dedicated simulator only; never installs on a connected device or existing DZlog app ID.
xcrun simctl boot "$device_id" || true
xcrun simctl bootstatus "$device_id" -b
xcrun simctl status_bar "$device_id" override --time '9:41' --batteryState charged --batteryLevel 100
xcrun simctl install "$device_id" "$repo_root/build/ios-poc/DZlogUiPoc.app"
xcrun simctl launch "$device_id" com.dudoziworkshop.dzlog.poc | tee "$evidence_dir/launch.txt"
# Give the first composition and iOS fonts time to settle before screenshot capture.
sleep 8
xcrun simctl spawn "$device_id" launchctl list | grep com.dudoziworkshop.dzlog.poc > "$evidence_dir/process.txt"
xcrun simctl io "$device_id" screenshot "$evidence_dir/template-preview.png"
xcrun simctl spawn "$device_id" log show --last 1m --style compact --predicate 'process == "DZlogUiPoc"' > "$evidence_dir/app.log"
xcrun simctl terminate "$device_id" com.dudoziworkshop.dzlog.poc
xcrun simctl shutdown "$device_id"
