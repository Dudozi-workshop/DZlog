#!/usr/bin/env python3
"""Screen-size, orientation-lock, and keyboard smoke on disposable API 36 emulator.

This tests UI reachability and screenshots at compact and tablet-like sizes.
It does not assert pixel-perfect layout or claim full split-screen coverage.
"""
import re
import time

from android16_journey_smoke import (
    ACTIVITY, OUT, PKG, adb, alive, contains, evidence, hierarchy, tap, target, wait_for,
)


def main():
    api = adb("shell", "getprop", "ro.build.version.sdk").strip()
    assert api == "36", f"Expected isolated Android 16 emulator, got {api}"
    rotation = adb("shell", "settings", "get", "system", "user_rotation").strip()
    auto_rotate = adb("shell", "settings", "get", "system", "accelerometer_rotation").strip()
    try:
        # Default DZlog preference locks portrait, even when device rotation is requested.
        adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
        adb("shell", "settings", "put", "system", "user_rotation", "1")
        start_home()
        evidence("33-device-rotation-portrait-lock")
        assert alive()

        for label, size, dpi in (
            ("compact", "720x1600", "360"),
            ("large", "1600x2560", "320"),
        ):
            adb("shell", "wm", "size", size)
            adb("shell", "wm", "density", dpi)
            time.sleep(3)
            start_home()
            root = wait_for("촬영 시작", timeout=45)
            x, y = target(root, "촬영 시작")
            top = next(iter(root.iter("node")))
            match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                                 top.get("bounds") or "")
            if match:
                _, _, w, h = (int(v) for v in match.groups())
                assert 0 <= x < w and 0 <= y < h, (
                    f"{label}: home camera action outside screen ({x},{y})")
            evidence(f"34-{label}-home")
            tap("촬영 시작")
            camera = wait_for("촬영 도구", timeout=45)
            assert contains(camera, "촬영 도구") or contains(camera, "●"), (
                f"{label}: camera controls missing")
            evidence(f"35-{label}-camera")

            adb("shell", "input", "keyevent", "4")
            wait_for("촬영 시작")
            # Wait for resize/rotation lifecycle transitions to settle before
            # navigating again. Treat a process exit as a real regression.
            time.sleep(2)
            alive()
            wait_for("설정", timeout=25)
            tap("설정")
            wait_for("사진 품질")
            evidence(f"36-{label}-settings")
            adb("shell", "input", "keyevent", "4")
            wait_for("촬영 시작")
            tap("앨범 보기 >")
            wait_for("갤러리")
            evidence(f"37-{label}-gallery")
            assert alive()

        # Keep the CI run non-destructive: return original display/rotation even
        # when a layout assertion fails. Existing photo/media are untouched.
        print("PASS Android16 responsive smoke: portrait lock, compact phone, "
              "large screen, controls reachable, settings/gallery navigation", flush=True)
    finally:
        adb("shell", "wm", "size", "reset")
        adb("shell", "wm", "density", "reset")
        if rotation in ("0", "1", "2", "3"):
            adb("shell", "settings", "put", "system", "user_rotation", rotation)
        if auto_rotate in ("0", "1"):
            adb("shell", "settings", "put", "system", "accelerometer_rotation", auto_rotate)


def start_home():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작", timeout=45)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Distinguish Android launcher return, activity crash and UI timing.
        try:
            evidence("FAILED-responsive")
            (OUT / "FAILED-responsive-process.txt").write_text(
                adb("shell", "dumpsys", "activity", "activities", timeout=50),
                encoding="utf-8",
            )
            (OUT / "FAILED-responsive-logcat.txt").write_text(
                adb("logcat", "-d", "-t", "2000", timeout=50),
                encoding="utf-8",
            )
        except Exception:
            pass
        raise
