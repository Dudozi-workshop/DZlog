#!/usr/bin/env python3
"""Android 16 non-destructive same-build update-install regression in CI.

This tests adb install -r of the current debug APK on a disposable emulator
with already populated data. It does NOT prove migration from the released
Google Play version, which needs its actual previous release artifact.
"""
import subprocess
import time
from pathlib import Path

from android16_journey_smoke import (
    ACTIVITY, PKG, adb, alive, contains, evidence, hierarchy, tap, wait_for,
)
from android16_capture_media_smoke import (
    check_both, query_media, rows_after, save_diagnostics,
)

APK = Path("app/build/outputs/apk/debug/app-debug.apk")


def photo_metadata(rows):
    return sorted((
        r["_id"], r["_display_name"], r["relative_path"],
        r["_size"], r["mime_type"], r["is_pending"],
    ) for r in rows)


def main():
    assert adb("shell", "getprop", "ro.build.version.sdk").strip() == "36"
    assert APK.is_file(), f"Debug APK missing: {APK}"
    before = query_media()
    assert len(before) >= 14 and len(before) % 2 == 0, (
        "Update-install QA requires earlier capture smoke, refuses fresh storage"
    )
    shots = len(before) // 2
    check_both(before, shots)
    snapshot = photo_metadata(before)

    # Replace the app package without uninstalling or clearing its data.
    result = subprocess.run(
        ["adb", "install", "-r", str(APK)],
        check=True, capture_output=True, text=True, timeout=150,
    )
    if "Success" not in result.stdout:
        raise AssertionError(f"APK replace-install failed: {result.stdout}")
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    home = wait_for("촬영 시작", timeout=50)
    if contains(home, "시작하기"):
        raise AssertionError("Starter onboarding repeated after update install")
    if not alive():
        raise AssertionError("App not alive after replace-install")

    after_install = query_media()
    check_both(after_install, shots)
    if photo_metadata(after_install) != snapshot:
        raise AssertionError("Replace-install changed original photo metadata")
    evidence("39-update-install-media-retained")

    # Confirm saved camera naming/counter is still readable for a new capture.
    tap("촬영 시작", "●")
    time.sleep(9)
    tap("●")
    after_new_capture = rows_after((shots + 1) * 2)
    check_both(after_new_capture, shots + 1)
    evidence("40-update-install-next-counter")
    print(
        f"PASS Android16 replace-install: previous {len(before)} JPEGs intact, "
        f"onboarding not repeated, new capture counter {shots + 1} preserved",
        flush=True,
    )


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            save_diagnostics("FAILED-update-install")
        finally:
            raise
