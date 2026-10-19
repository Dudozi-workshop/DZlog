#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
framework_dir="$repo_root/sharedUi/build/bin/iosSimulatorArm64/debugFramework"
app_dir="$repo_root/build/ios-poc/DZlogUiPoc.app"
mkdir -p "$app_dir/Frameworks"
xcrun --sdk iphonesimulator swiftc -parse-as-library \
  -sdk "$(xcrun --sdk iphonesimulator --show-sdk-path)" \
  -target arm64-apple-ios15.0-simulator \
  -F "$framework_dir" -framework DZlogUi \
  -Xlinker -rpath -Xlinker @executable_path/Frameworks \
  "$repo_root/iosApp/PoCApp/AppDelegate.swift" -o "$app_dir/DZlogUiPoc"
cp "$repo_root/iosApp/PoCApp/Info.plist" "$app_dir/Info.plist"
ditto "$framework_dir/DZlogUi.framework" "$app_dir/Frameworks/DZlogUi.framework"
codesign --force --sign - "$app_dir/Frameworks/DZlogUi.framework"
codesign --force --sign - "$app_dir"
