#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! $1 =~ ^[A-Z0-9]{10}$ ]]; then
  echo "Usage: bash scripts/ios/archive-for-testflight.sh APPLE_TEAM_ID" >&2
  exit 2
fi
team_id="$1"
if ! command -v xcodebuild >/dev/null || ! xcrun --sdk iphoneos --show-sdk-path >/dev/null; then
  echo "The iPhone SDK from Xcode is required to archive DZlog." >&2
  exit 1
fi

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$repo_root"
archive_dir="$(mktemp -d "${TMPDIR:-/tmp}/dzlog-archive.XXXXXXXX")"
archive_path="$archive_dir/DZlog.xcarchive"

bash ./gradlew -PdzlogSharedOnly=true -PdzlogUiPoc=true :sharedUi:linkReleaseFrameworkIosArm64
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
