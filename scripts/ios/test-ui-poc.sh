#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
test_scope="${DZLOG_UI_TEST_SCOPE:-full}"
case "$test_scope" in
  settings) selection=(-only-testing:DZlogUiPocUITests/PreviewInteractionTests/testSettingsQualityAccessibilityAndPersistence) ;;
  media) selection=(-only-testing:DZlogUiPocUITests/PreviewInteractionTests/testMediaFixturePhotoKitSaveAndRelaunch) ;;
  editor) selection=(-only-testing:DZlogUiPocUITests/PreviewInteractionTests/testEditorContentStructureStyleSaveAndRelaunch) ;;
  navigation) selection=(
    -only-testing:DZlogUiPocUITests/PreviewInteractionTests/testEditorContentStructureStyleSaveAndRelaunch
    -only-testing:DZlogUiPocUITests/PreviewInteractionTests/testHomeCatalogSettingsAndRelaunch
  ) ;;
  full) selection=() ;;
  *) echo "Unsupported UI scope: $test_scope" >&2; exit 2 ;;
esac
evidence_dir="$repo_root/build/ios-poc/evidence"
mkdir -p "$evidence_dir"
printf '%s\n' "$test_scope" > "$evidence_dir/test-scope.txt"
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
# Bound native XCTest execution and preserve a partial transcript on simulator stalls.
case "$test_scope" in
  settings) limit_seconds=480 ;;
  media) limit_seconds=600 ;;
  editor) limit_seconds=720 ;;
  navigation) limit_seconds=900 ;;
  full) limit_seconds=1200 ;;
esac
python3 "$repo_root/scripts/ios/run-capped-command.py" "$limit_seconds" \
  "$evidence_dir/interaction-build.log" \
  xcodebuild test-without-building "${xcode_args[@]}" ${selection[@]+"${selection[@]}"} \
    -resultBundlePath "$evidence_dir/interaction.xcresult"
