#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"

python3 - "$repo_root" <<'PY'
import json
import plistlib
import struct
import sys
from pathlib import Path

root = Path(sys.argv[1])
app = root / "iosApp/PoCApp"
info = plistlib.loads((app / "Info.plist").read_bytes())
privacy = plistlib.loads((app / "PrivacyInfo.xcprivacy").read_bytes())

assert info["CFBundleIdentifier"] == "com.dudoziworkshop.dzlog"
assert info["CFBundleDisplayName"] == "DZlog"
assert info["CFBundleShortVersionString"] == "1.0.0"
assert info["CFBundleVersion"] == "3"
assert info["ITSAppUsesNonExemptEncryption"] is False
assert info["NSCameraUsageDescription"].strip()
assert info["PHPhotoLibraryPreventAutomaticLimitedAccessAlert"] is True
assert info["NSPhotoLibraryUsageDescription"].strip()
assert info["NSPhotoLibraryAddUsageDescription"].strip()

assert privacy["NSPrivacyTracking"] is False
assert privacy["NSPrivacyTrackingDomains"] == []
assert privacy["NSPrivacyCollectedDataTypes"] == []
reasons = {
    item["NSPrivacyAccessedAPIType"]: set(item["NSPrivacyAccessedAPITypeReasons"])
    for item in privacy["NSPrivacyAccessedAPITypes"]
}
assert reasons == {
    "NSPrivacyAccessedAPICategoryUserDefaults": {"CA92.1"},
    "NSPrivacyAccessedAPICategorySystemBootTime": {"35F9.1"},
}

assets = app / "Assets.xcassets"
for catalog in assets.rglob("Contents.json"):
    json.loads(catalog.read_text())

icon = assets / "AppIcon.appiconset/AppIcon-1024.png"
with icon.open("rb") as stream:
    assert stream.read(8) == b"\x89PNG\r\n\x1a\n"
    length = struct.unpack(">I", stream.read(4))[0]
    assert stream.read(4) == b"IHDR" and length == 13
    width, height, _, color_type, _, _, _ = struct.unpack(">IIBBBBB", stream.read(13))
assert (width, height) == (1024, 1024)
assert color_type == 2, "App Store icon must be opaque RGB without alpha"

project = (root / "iosApp/DZlogUiPoc.xcodeproj/project.pbxproj").read_text()
assert "PoCApp/PrivacyInfo.xcprivacy" in project
assert "ASSETCATALOG_COMPILER_APPICON_NAME" in project
for script_name in ("build-ui-poc.sh", "build-device-poc.sh"):
    script = (root / "scripts/ios" / script_name).read_text()
    assert "PrivacyInfo.xcprivacy" in script

print("iOS production identity, brand assets, and privacy metadata are valid.")
PY
