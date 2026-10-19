#!/usr/bin/env python3
"""Disposable Android 16 integration regression for approved DZlog onboarding.

Never run this against a real phone or existing user photos. This script installs
only into a clean CI emulator and tests real CameraX/MediaStore photo pairs, a
short capture burst, permission recovery, and same-build replace-install.
It is NOT a Google Play old-release migration test.
"""
import subprocess
import time
from pathlib import Path

from android16_journey_smoke import (
    ACTIVITY, OUT, PKG, adb, alive, contains, evidence, hierarchy, wait_for,
)
from dzlog_onboarding_ui_visual_smoke import (
    folder_setup, tap_visible,
)
import android16_capture_media_smoke
import android16_rapid_shutter_smoke
import android16_permission_return_smoke
import android16_update_install_smoke


def fresh_setup():
    assert adb("shell", "getprop", "ro.build.version.sdk").strip() == "36"
    assert not adb("shell", "pm", "list", "packages", PKG).strip(), (
        "Refusing to run on an emulator with an existing DZlog installation"
    )
    apk = Path("app/build/outputs/apk/debug/app-debug.apk")
    assert apk.is_file(), "Integration QA APK not built"

    deadline = time.monotonic() + 100
    while time.monotonic() < deadline:
        try:
            adb("shell", "mkdir", "-p", "/sdcard/Pictures/DZlog", timeout=15)
            break
        except (RuntimeError, subprocess.TimeoutExpired):
            time.sleep(2)
    else:
        raise RuntimeError("Disposable emulator storage unavailable")

    install = subprocess.run(
        ["adb", "install", str(apk)], capture_output=True, text=True, timeout=180,
    )
    if install.returncode or "Success" not in install.stdout:
        raise RuntimeError("Failed to install test APK: " + install.stderr[-900:])

    # Only on the disposable emulator; never alter a person's permissions.
    for permission in (
        "android.permission.CAMERA", "android.permission.READ_MEDIA_IMAGES"
    ):
        adb("shell", "pm", "grant", PKG, permission)

    # The approved compact-layout checks use a 720x1600 physical-pixel viewport.
    # The emulator runner otherwise boots Pixel 6 at 1080x2400.
    adb("shell", "wm", "size", "720x1600")
    adb("shell", "wm", "density", "360")
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    adb("shell", "am", "start", "-W", "-n", ACTIVITY, timeout=90)
    wait_for("사진에 기록을 더해,", timeout=90)
    evidence("integration-01-intro")
    tap_visible("시작하기")
    wait_for("사진에 기록표를 겹친 촬영 예시", timeout=65)
    tap_visible("사용 준비하기")
    folder_setup()

    # Choose the simple starter to validate default filename/counter behavior.
    wait_for("어떤 기록을 시작할까요?", timeout=70)
    if not contains(hierarchy(), "기본 촬영"):
        raise AssertionError("Approved basic starter category absent")
    tap_visible("기본 촬영")
    tap_visible("DZlog 시작하기")
    wait_for("사용법 시작", timeout=75)
    tap_visible("나중에")
    wait_for("촬영 시작", timeout=65)
    evidence("integration-02-first-home")

    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY, timeout=90)
    home = wait_for("촬영 시작", timeout=75)
    if contains(home, "어떤 기록을 시작할까요?"):
        raise AssertionError("Onboarding repeated after first setup")
    alive()
    evidence("integration-03-home-relaunch")


def main():
    fresh_setup()
    print("CHECK 1: approved onboarding -> home -> relaunch PASS", flush=True)
    android16_capture_media_smoke.main()
    print("CHECK 2: 5 real shutters / 10 original+result JPEGs PASS", flush=True)
    android16_rapid_shutter_smoke.main()
    print("CHECK 3: burst / capture gate recovery PASS", flush=True)
    android16_permission_return_smoke.main()
    print("CHECK 4: permission deny/regrant and media retained PASS", flush=True)
    android16_update_install_smoke.main()
    print("CHECK 5: update-install retains original+result JPEGs, counters PASS", flush=True)
    evidence("integration-05-final")
    print("PASS: isolated Android 16 onboarding/capture/permissions/update regression", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            evidence("FAILED-onboarding-integrated-capture")
        except Exception:
            pass
        raise
