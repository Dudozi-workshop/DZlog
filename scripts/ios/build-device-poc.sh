#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
framework_dir="$repo_root/sharedUi/build/bin/iosArm64/debugFramework"
out_dir="$repo_root/build/ios-device-poc"
app_dir="$out_dir/DZlogUiPoc.app"
evidence_dir="$out_dir/evidence"
asset_info="$out_dir/asset-info.plist"

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
cp "$repo_root/iosApp/PoCApp/PrivacyInfo.xcprivacy" "$app_dir/PrivacyInfo.xcprivacy"
cp "$repo_root/iosApp/PoCApp/home_leaf_shadow_c.png" "$app_dir/home_leaf_shadow_c.png"
xcrun --sdk iphoneos actool "$repo_root/iosApp/PoCApp/Assets.xcassets" \
  --compile "$app_dir" \
  --platform iphoneos \
  --minimum-deployment-target 15.0 \
  --target-device iphone \
  --target-device ipad \
  --app-icon AppIcon \
  --output-partial-info-plist "$asset_info"
/usr/libexec/PlistBuddy -c "Merge $asset_info" "$app_dir/Info.plist"
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

# AltStore Classic and public cloud device farms replace the signature and
# provisioning profile. Package a structurally valid device-target IPA without
# Apple credentials. It is not directly installable by tapping it in Safari.
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

# AltStore Classic on Windows accepts an IPA and applies the user's free Apple
# provisioning locally. Keep a plainly named copy so the tester does not need
# to understand the cloud-device package name.
altstore_ipa="$evidence_dir/DZlog-iPhone-free-test-AltStore.ipa"
cp "$evidence_dir/dzlog-ios-device-CLOUD-RESIGN.ipa" "$altstore_ipa"
cat > "$evidence_dir/README-ALTSTORE-WINDOWS-KO.txt" <<'EOF'
DZlog iPhone 무료 테스트판 (Windows + AltStore Classic)

이 IPA는 iPhone용 arm64 앱입니다. Safari에서 바로 설치하는 파일이 아닙니다.
Windows의 AltServer/AltStore Classic에서 무료 Apple 계정으로 다시 서명한 뒤
iPhone에 설치해야 합니다.

준비물
- Windows PC
- iPhone과 데이터 케이블
- Apple 계정 (Apple Developer Program 유료 가입 불필요)
- Apple 공식 배포판 iTunes와 iCloud
- AltServer for Windows

설치 요약
1. Windows에 iTunes, iCloud, AltServer를 설치합니다.
2. iPhone을 연결하고 이 컴퓨터를 신뢰합니다.
3. AltServer에서 iPhone에 AltStore를 설치합니다.
4. iPhone에서 개발자 모드를 켭니다.
5. AltStore의 My Apps에서 +를 눌러
   DZlog-iPhone-free-test-AltStore.ipa를 선택합니다.
6. DZlog를 실행하고 사진 및 카메라 권한을 허용합니다.

무료 서명 제한
- 앱은 7일마다 AltServer를 통해 갱신해야 합니다.
- iPhone과 AltServer가 같은 Wi-Fi에 있거나 케이블로 연결되어야 합니다.
- 무료 계정은 동시에 설치 가능한 사이드로드 앱 수가 제한됩니다.

주의
- Apple 계정 암호나 인증 코드를 다른 사람에게 전달하지 마세요.
- 이 패키지에는 Apple 계정, 인증서, 프로비저닝 프로파일이 없습니다.
- 갱신 기한 전에 AltStore에서 Refresh All을 실행하는 것이 안전합니다.
EOF
{
  printf 'commit=%s\n' "$(git -C "$repo_root" rev-parse HEAD)"
  printf 'bundle_id=com.dudoziworkshop.dzlog\n'
  printf 'platform=iphoneos-arm64\n'
  printf 'minimum_ios=15.0\n'
  printf 'signing=AltStore Classic free Apple account required\n'
} > "$evidence_dir/ALTSTORE-BUILD-INFO.txt"
shasum -a 256 "$altstore_ipa" >> "$evidence_dir/sha256.txt"
