#!/usr/bin/env python3
"""Focused onboarding visual/accessibility journey on a disposable Android 16 emulator.

Verifies compact width, enlarged text, interactive photo/table illustration,
real Android SAF folder setup, starter preview, and non-repeating onboarding.
Never execute against a user's actual device or their photo collection.
"""
import re
import subprocess
import time
from pathlib import Path

from android16_journey_smoke import (
    ACTIVITY, OUT, PKG, adb, alive, contains, evidence, hierarchy, nodes,
    tap, target, wait_for,
)
from android16_backup_saf_smoke import picker

ARTIFACTS = Path("onboarding-ui-qa")
ARTIFACTS.mkdir(exist_ok=True)


def mark(label):
    root = evidence("onboarding-" + label)
    return root


def tap_visible(label, timeout=35):
    until = time.monotonic() + timeout
    while time.monotonic() < until:
        root = hierarchy()
        for node in nodes(root):
            if (node.get("text") or "") == label or (node.get("content-desc") or "") == label:
                try:
                    x, y = target(root, label)
                    adb("shell", "input", "tap", str(x), str(y))
                    return
                except AssertionError:
                    pass
        time.sleep(1)
    raise AssertionError("Tap target not visible: " + label)


def scroll_until(label, timeout=25):
    until = time.monotonic() + timeout
    while time.monotonic() < until:
        root = hierarchy()
        if contains(root, label):
            return root
        adb("shell", "input", "swipe", "340", "1180", "340", "480", "420")
        time.sleep(1)
    raise AssertionError("Scroll target not reachable: " + label)


def is_fully_on_screen(root, label):
    for node in nodes(root):
        if (node.get("text") or "") == label or (node.get("content-desc") or "") == label:
            m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds") or "")
            if not m:
                continue
            x1, y1, x2, y2 = [int(part) for part in m.groups()]
            # Smallest configured viewport is 720 x 1600 physical pixels.
            return x1 >= 0 and y1 >= 0 and x2 <= 720 and y2 <= 1600 and x2 > x1 and y2 > y1
    return False


def choose(label):
    scroll_until(label)
    tap_visible(label)


def wait_for_folder_grant_or_starter(timeout=90):
    """Only click exact affirmative system buttons, or accept a direct return.

    A substring search for "Allow" may match the dialog's sentence
    ("Allow access ...") even when no button labelled "Allow" exists.
    """
    deadline = time.monotonic() + timeout
    affirmative = {"allow", "허용", "액세스 허용", "접근 허용"}
    while time.monotonic() < deadline:
        root = hierarchy()
        if contains(root, "어떤 기록을 시작할까요?"):
            return root
        if contains(root, "선택한 위치가 달라요"):
            raise AssertionError("App rejected the selected SAF folder")
        for node in nodes(root):
            text = (node.get("text") or node.get("content-desc") or "").strip()
            if text.casefold() not in affirmative:
                continue
            try:
                x, y = target(root, text)
            except AssertionError:
                continue
            adb("shell", "input", "tap", str(x), str(y))
            time.sleep(1.5)
            break
        else:
            time.sleep(1.5)
    raise AssertionError("Neither SAF confirmation nor starter screen became available")


def folder_setup():
    # No permission dialogs are bypassed: Android permissions were granted
    # expressly within this disposable emulator before the app was launched.
    root = wait_for("앱을 사용하기 위해", timeout=60)
    if not contains(root, "저장 폴더 연결"):
        raise AssertionError("All required setup items should be present")
    if not is_fully_on_screen(root, "DZlog 폴더 연결"):
        raise AssertionError("Setup primary button clipped at compact size")
    mark("03-permissions")
    tap_visible("DZlog 폴더 연결")
    picker(timeout=90)
    # DocumentsUI may initially report its package before the folder list
    # has rendered. Always navigate using a fresh, settled hierarchy.
    mark("04-android-folder-picker")
    deadline = time.monotonic() + 45
    while True:
        root = hierarchy()
        if any(contains(root, name) for name in
               ("Pictures", "DZlog", "USE THIS FOLDER", "Use this folder")):
            break
        if time.monotonic() >= deadline:
            raise AssertionError("Android picker did not render a usable folder list")
        time.sleep(1.5)
    # Android 16 often ignores the picker initial URI and opens at device root.
    # Navigate only via visible, exact Pictures/DZlog folder entries.
    if contains(root, "Pictures"):
        choose("Pictures")
        picker(timeout=45)
        choose("DZlog")
    elif not contains(root, "DZlog"):
        raise AssertionError("Android picker did not expose Pictures/DZlog")
    root = picker(timeout=45)
    deadline = time.monotonic() + 40
    while not (contains(root, "USE THIS FOLDER") or
               contains(root, "Use this folder") or contains(root, "이 폴더 사용")):
        if time.monotonic() >= deadline:
            raise AssertionError("Pictures/DZlog folder has no selection action")
        time.sleep(1.5)
        root = hierarchy()
    for label in ("USE THIS FOLDER", "Use this folder", "이 폴더 사용"):
        if contains(root, label):
            tap_visible(label)
            break
    else:
        raise AssertionError("Exact folder is not selectable")
    wait_for_folder_grant_or_starter(timeout=90)


def main():
    assert adb("shell", "getprop", "ro.build.version.sdk").strip() == "36"
    # Fresh emulator only: install without touching any existing Play application.
    assert not adb("shell", "pm", "list", "packages", PKG).strip(), "Emulator is not empty"
    adb("shell", "mkdir", "-p", "/sdcard/Pictures/DZlog", timeout=35)
    apk = "app/build/outputs/apk/debug/app-debug.apk"
    proc = subprocess.run(["adb", "install", apk], capture_output=True, text=True, timeout=180)
    if proc.returncode != 0:
        raise RuntimeError("APK installation failed: " + proc.stderr[-600:])
    for permission in ("android.permission.CAMERA", "android.permission.READ_MEDIA_IMAGES"):
        adb("shell", "pm", "grant", PKG, permission)
    old_size = adb("shell", "wm", "size").strip()
    old_density = adb("shell", "wm", "density").strip()
    old_font = adb("shell", "settings", "get", "system", "font_scale").strip()
    try:
        adb("shell", "wm", "size", "720x1600")
        adb("shell", "wm", "density", "360")
        # Visual-fit preflight at standard text scaling. Reset only the
        # disposable emulator app before the existing large-text E2E run.
        adb("shell", "settings", "put", "system", "font_scale", "1.0")
        adb("shell", "am", "start", "-W", "-n", ACTIVITY, timeout=90)
        root = wait_for("사진에 기록을 더해,", timeout=85)
        if contains(root, "건너뛰기"):
            raise AssertionError("First introduction must not offer Skip")
        if not is_fully_on_screen(root, "시작하기"):
            raise AssertionError("Start button must remain fully visible")
        if not is_fully_on_screen(root, "한 장의 사진이, 더 오래 기억되는 기록이 됩니다."):
            raise AssertionError("Intro composition needs scrolling at normal font size")
        mark("00-intro-normal-font")
        tap_visible("시작하기")
        root = wait_for("사진과 기록, 내 방식대로.", timeout=55)
        if not is_fully_on_screen(root, "쌓인 기록도, 찾기 쉽게."):
            raise AssertionError("Third feature card needs scrolling at normal font size")
        mark("00-feature-normal-font")
        # Finish the NORMAL-font journey before switching to accessibility
        # scale. Compose may keep the activity's previous fontScale after
        # a settings change, making a mid-activity check a false failure.
        tap_visible("사용 준비하기")
        wait_for("앱을 사용하기 위해", timeout=75)
        mark("00-permissions-normal-font")
        folder_setup()
        choose("업무·현장")
        normal_starter = hierarchy()
        if not is_fully_on_screen(normal_starter, "저장 규칙 미리보기"):
            raise AssertionError("Starter photo/save preview needs scrolling at normal font size")
        mark("00-starter-normal-font")
        adb("shell", "am", "force-stop", PKG)
        adb("shell", "pm", "clear", PKG)
        for permission in ("android.permission.CAMERA", "android.permission.READ_MEDIA_IMAGES"):
            adb("shell", "pm", "grant", PKG, permission)
        adb("shell", "settings", "put", "system", "font_scale", "1.4")
        adb("shell", "am", "start", "-W", "-n", ACTIVITY, timeout=90)
        root = wait_for("사진에 기록을 더해,", timeout=85)
        if not is_fully_on_screen(root, "시작하기"):
            raise AssertionError("Compact onboarding intro CTA clipped")
        mark("01-intro-compact-large-font")
        tap_visible("시작하기")
        wait_for("사진과 기록, 내 방식대로.", timeout=55)
        wait_for("사진에 기록표를 겹친 촬영 예시")
        mark("02-feature-reference-cards")
        if not is_fully_on_screen(hierarchy(), "사용 준비하기"):
            raise AssertionError("Fixed onboarding CTA clipped after scrolling")
        tap_visible("사용 준비하기")
        folder_setup()

        # Selection only: changing category must not create or save templates.
        choose("업무·현장")
        # Large 140%-font mode may scroll but all controls stay reachable.
        mark("05-starter-large-font")
        choose("다른 양식 보기")
        choose("연구·실험")
        root = wait_for("연구·실험", timeout=40)
        if not contains(root, "DZlog 시작하기"):
            raise AssertionError("Starter confirmation CTA missing")
        mark("05-starter-selection-compact")
        tap_visible("DZlog 시작하기")
        # Initial setup must enter home, never force the camera.
        root = wait_for("사용법 시작", timeout=80)
        if not contains(root, "나중에"):
            raise AssertionError("First-home tutorial offer missing")
        mark("06-home-tutorial-invitation")
        tap_visible("나중에")
        if contains(wait_for("촬영 시작", timeout=60), "사용법 시작"):
            raise AssertionError("Tutorial invitation was not dismissed")
        adb("shell", "am", "force-stop", PKG)
        adb("shell", "am", "start", "-W", "-n", ACTIVITY, timeout=90)
        wait_for("촬영 시작", timeout=75)
        root = mark("07-relaunch-home")
        if contains(root, "사진에 기록을 더해,") or contains(root, "어떤 기록을 시작할까요?"):
            raise AssertionError("Onboarding repeated after completing setup")
        # Use an ACTUAL camera screen capture for the approved marketing mockup.
        # This only visits the camera on the disposable emulator. No shutter,
        # gallery deletion, or photo file modifications are performed.
        tap_visible("촬영 시작")
        time.sleep(2.5)
        camera_root = mark("08-actual-dzlog-camera")
        if contains(camera_root, "사진에 기록을 더해,"):
            raise AssertionError("Camera navigation unexpectedly reopened onboarding")
        alive()
        print("PASS: 4-stage onboarding, starter, permissions, relaunch and real camera preview")
    finally:
        try:
            adb("shell", "wm", "size", "reset")
            adb("shell", "wm", "density", "reset")
            if old_font not in ("", "null"):
                adb("shell", "settings", "put", "system", "font_scale", old_font)
        except Exception:
            pass


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            mark("FAILED-last-screen")
        except Exception:
            pass
        raise
