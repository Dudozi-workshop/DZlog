#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
framework_dir="$repo_root/sharedUi/build/bin/iosSimulatorArm64/debugFramework"
app_dir="$repo_root/build/ios-poc/DZlogUiPoc.app"
asset_info="$repo_root/build/ios-poc/asset-info.plist"
mkdir -p "$app_dir/Frameworks"
xcrun --sdk iphonesimulator swiftc -parse-as-library \
  -sdk "$(xcrun --sdk iphonesimulator --show-sdk-path)" \
  -target arm64-apple-ios15.0-simulator \
  -F "$framework_dir" -framework DZlogUi \
  -Xlinker -rpath -Xlinker @executable_path/Frameworks \
  "$repo_root/iosApp/PoCApp/AppDelegate.swift" -o "$app_dir/DZlogUiPoc"
cp "$repo_root/iosApp/PoCApp/Info.plist" "$app_dir/Info.plist"
cp "$repo_root/iosApp/PoCApp/PrivacyInfo.xcprivacy" "$app_dir/PrivacyInfo.xcprivacy"
cp "$repo_root/iosApp/PoCApp/home_leaf_shadow_c.png" "$app_dir/home_leaf_shadow_c.png"
xcrun --sdk iphonesimulator actool "$repo_root/iosApp/PoCApp/Assets.xcassets" \
  --compile "$app_dir" \
  --platform iphonesimulator \
  --minimum-deployment-target 15.0 \
  --target-device iphone \
  --target-device ipad \
  --app-icon AppIcon \
  --output-partial-info-plist "$asset_info"
/usr/libexec/PlistBuddy -c "Merge $asset_info" "$app_dir/Info.plist"
ditto "$framework_dir/DZlogUi.framework" "$app_dir/Frameworks/DZlogUi.framework"
codesign --force --sign - "$app_dir/Frameworks/DZlogUi.framework"
codesign --force --sign - "$app_dir"
