#!/usr/bin/env python3
"""Non-destructive Android 16 system navigation/inset smoke on disposable CI.

Attempts to enable the real Android three-button system navigation overlay.
If unsupported by the emulator, emits SKIP (never calls that a pass).
Captures before/after evidence and restores the original navigation overlay.
"""
import re
import time

from android16_journey_smoke import (
    ACTIVITY, PKG, OUT, adb, alive, contains, evidence, hierarchy, tap, target, wait_for,
)
from android16_editor_keyboard_smoke import keyboard_is_visible

THREE_BUTTON = "com.android.internal.systemui.navbar.threebutton"
GESTURAL = "com.android.internal.systemui.navbar.gestural"


def overlay_status():
    # Both overlay packages are Android system overlays, not app settings.
    return adb("shell", "cmd", "overlay", "list", "--user", "0", "android", timeout=60)


def enabled_package(status):
    for package in (THREE_BUTTON, GESTURAL):
        if re.search(r"\[x\]\s+" + re.escape(package) + r"(?:\s|$)", status):
            return package
    return None


def set_navigation(package):
    adb("shell", "cmd", "overlay", "enable-exclusive", "--user", "0",
        "--category", package, timeout=65)
    time.sleep(4)
    return enabled_package(overlay_status()) == package


def display_size():
    state = adb("shell", "wm", "size")
    match = re.search(r"(?:Override|Physical) size:\s*(\d+)x(\d+)", state)
    if not match:
        raise AssertionError(f"Cannot read display size: {state}")
    return tuple(int(s) for s in match.groups())


def display_density():
    state = adb("shell", "wm", "density")
    match = re.search(r"(?:Override|Physical) density:\s*(\d+)", state)
    if not match:
        raise AssertionError(f"Cannot read display density: {state}")
    return int(match.group(1))


def start_home():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작", timeout=55)


def main():
    assert adb("shell", "getprop", "ro.build.version.sdk").strip() == "36"
    before = overlay_status()
    (OUT / "41-navbar-original-overlay.txt").write_text(before, encoding="utf-8")
    if THREE_BUTTON not in before:
        print("SKIP Android16 three-button nav: overlay unavailable on emulator", flush=True)
        return
    original = enabled_package(before)
    if not original:
        print("SKIP Android16 three-button nav: original navigation mode unknown", flush=True)
        return

    try:
        if not set_navigation(THREE_BUTTON):
            print("SKIP Android16 three-button nav: system refused overlay activation", flush=True)
            return

        start_home()
        evidence("42-threebutton-home")
        tap("촬영 시작")
        camera = wait_for("촬영 도구", timeout=45)
        x, y = target(camera, "●")
        width, height = display_size()
        dpi = display_density()
        nav_reserved = 48 * dpi / 160.0
        if not (0 < x < width and 0 < y < height - nav_reserved):
            raise AssertionError(
                f"Camera shutter ({x},{y}) overlaps three-button nav zone: "
                f"{width}x{height}, density {dpi}"
            )
        evidence("43-threebutton-camera")

        tap("표 편집")
        wait_for("셀을 선택해 내용을 편집하세요.")
        tap("기록명")
        wait_for("값")
        tap("값")
        time.sleep(2)
        root = evidence("44-threebutton-editor-ime")
        if not keyboard_is_visible(root):
            raise AssertionError("IME hidden during three-button navigation test")
        if contains(root, "저장설정"):
            raise AssertionError("Bottom editor toolbar overlaps opened keyboard")
        adb("shell", "input", "keyevent", "4")
        wait_for("저장설정")
        evidence("45-threebutton-editor-ime-closed")
        if not alive():
            raise AssertionError("DZlog process terminated after system nav/IME change")
        print("PASS Android16 three-button nav: shutter outside system bar, "
              "editor keyboard available, bottom toolbar hidden during IME", flush=True)
    finally:
        if not set_navigation(original):
            raise AssertionError(f"Could not restore original system navbar: {original}")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            evidence("FAILED-navigation-insets")
            (OUT / "FAILED-navigation-insets-window.txt").write_text(
                adb("shell", "dumpsys", "window", timeout=55), encoding="utf-8",
            )
        except Exception:
            pass
        raise
