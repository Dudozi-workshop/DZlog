#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! $1 =~ ^[A-Z0-9]{10}$ ]]; then
  echo "Usage: bash scripts/ios/archive-for-testflight.sh APPLE_TEAM_ID" >&2
  exit 2
fi
team_id="$1"
if ! command -v xcodebuild >/dev/null ||
  ! xcode_version="$(xcodebuild -version)" ||
  ! ios_sdk_version="$(xcrun --sdk iphoneos --show-sdk-version)"; then
  echo "Xcode with the iPhone SDK is required to archive DZlog." >&2
  exit 1
fi
xcode_version="${xcode_version%%$'\n'*}"
if [[ ! "$xcode_version" =~ ^Xcode\ ([0-9]+)(\.|$) ]]; then
  echo "Could not determine the selected Xcode version: $xcode_version" >&2
  exit 1
fi
xcode_major="${BASH_REMATCH[1]}"
sdk_major="${ios_sdk_version%%.*}"
if [[ ! "$sdk_major" =~ ^[0-9]+$ ]] || (( xcode_major < 26 || sdk_major < 26 )); then
  echo "TestFlight requires Xcode 26 or later with iOS SDK 26 or later. Found $xcode_version, iOS SDK $ios_sdk_version." >&2
  exit 1
fi

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$repo_root"
archive_dir="$(mktemp -d "${TMPDIR:-/tmp}/dzlog-archive.XXXXXXXX")"
archive_path="$archive_dir/DZlog.xcarchive"

# The pinned Kotlin/Native compiler can use its own compatible Xcode
# installation. The final app archive still uses the selected upload SDK.
framework_args=(-PdzlogSharedOnly=true -PdzlogUiPoc=true :sharedUi:linkReleaseFrameworkIosArm64)
if [[ -n "${DZLOG_KOTLIN_DEVELOPER_DIR:-}" ]]; then
  DEVELOPER_DIR="$DZLOG_KOTLIN_DEVELOPER_DIR" bash ./gradlew "${framework_args[@]}"
else
  bash ./gradlew "${framework_args[@]}"
fi
xcodebuild archive \
  -project iosApp/DZlogUiPoc.xcodeproj \
  -scheme DZlogUiPoc \
  -destination 'generic/platform=iOS' \
  -archivePath "$archive_path" \
  -allowProvisioningUpdates \
  DEVELOPMENT_TEAM="$team_id"

app_path="$archive_path/Products/Applications/DZlogUiPoc.app"
test -d "$app_path"
codesign --verify --deep --strict "$app_path"
printf 'Signed archive for Xcode Organizer: %s\n' "$archive_path"
