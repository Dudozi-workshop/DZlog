#!/usr/bin/env python3
"""Integrated Android 16 release journey on an isolated, disposable CI emulator.

Checks mandatory SAF setup, first-run onboarding, JPEG BOTH captures,
gallery navigation, and real SAF backup export/import. Never use with user data.
"""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

from android16_journey_smoke import (
    PKG, OUT, adb, alive, contains, evidence, hierarchy, nodes,
    target, tap, wait_for,
)
from android16_backup_saf_smoke import picker, open_settings, export_backup, import_backup
from android16_capture_media_smoke import main as capture_media_main, check_both, query_media

ACTIVITY = f"{PKG}/.MainActivity"
EVIDENCE = Path("postrelease-full-e2e")
EVIDENCE.mkdir(exist_ok=True)


def mark(name):
    """Persist a named checkpoint even when a subsequent assertion fails."""
    root = hierarchy()
    ET.ElementTree(root).write(EVIDENCE / f"{name}.xml", encoding="utf-8")
    (EVIDENCE / f"{name}.png").write_bytes(
        subprocess.run(["adb", "exec-out", "screencap", "-p"],
                       check=True, capture_output=True, timeout=45).stdout
    )
    return root


def tap_any(labels, timeout=30):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        root = hierarchy()
        for label in labels:
            for item in nodes(root):
                content = (item.get("text") or item.get("content-desc") or "").strip()
                if content.casefold() == label.casefold():
                    x, y = target(root, content)
                    adb("shell", "input", "tap", str(x), str(y))
                    return label
        time.sleep(1.5)
    raise AssertionError(f"Expected clickable label not found: {labels}")


def install_once_ready():
    assert adb("shell", "getprop", "ro.build.version.sdk").strip() == "36"
    adb("shell", "mkdir", "-p", "/sdcard/Pictures/DZlog", timeout=45)
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        try:
            if (adb("shell", "getprop", "sys.boot_completed", timeout=20).strip() == "1"
                and "found" in adb("shell", "service", "check", "package", timeout=20).lower()):
                break
        except (subprocess.TimeoutExpired, RuntimeError):
            pass
        time.sleep(3)
    else:
        raise RuntimeError("Android package manager unavailable")
    apk = "app/build/outputs/apk/debug/app-debug.apk"
    result = subprocess.run(["adb", "install", "-r", apk], capture_output=True, text=True, timeout=180)
    if result.returncode or "Success" not in result.stdout:
        raise RuntimeError(f"APK install failed: {result.stdout[-400:]} {result.stderr[-400:]}")
    for permission in ("android.permission.CAMERA", "android.permission.READ_MEDIA_IMAGES"):
        adb("shell", "pm", "grant", PKG, permission)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY, timeout=70)


def link_exact_folder():
    root = wait_for("DZlog 폴더 연결", timeout=90)
    if contains(root, "촬영 시작"):
        raise AssertionError("Mandatory SAF gate bypassed on new installation")
    mark("01-mandatory-folder")
    tap("DZlog 폴더 연결", "DZlog 폴더를 연결해 주세요")
    mark("02-folder-guide")
    tap_any(["폴더 선택 화면으로", "다시 연결하기"])
    root = picker(timeout=90)
    mark("03-saf-tree-picker")
    # Some Android 16 DocumentsUI versions ignore the initial DZlog URI and
    # instead open at the emulated device root, where folder use is disabled.
    # Navigate via the real Pictures folder before selecting DZlog itself.
    if contains(root, "Can't use this folder") or contains(root, "Can’t use this folder"):
        tap_any(["Pictures"])
        root = picker(timeout=45)
        mark("03b-saf-pictures")
        if not contains(root, "DZlog"):
            raise AssertionError("Pictures/DZlog folder not visible in SAF picker")
        tap_any(["DZlog"])
        root = picker(timeout=45)
        mark("03c-saf-exact-dzlog")
    elif not contains(root, "DZlog"):
        if contains(root, "Pictures"):
            tap_any(["Pictures"])
            root = picker(timeout=45)
            if not contains(root, "DZlog"):
                raise AssertionError("Pictures/DZlog folder missing")
            tap_any(["DZlog"])
            root = picker(timeout=45)
        else:
            raise AssertionError("SAF picker did not expose DZlog or Pictures")
    # App-side GalleryFolderStorage.connect also rejects every non-exact tree.
    if not contains(root, "DZlog") or not contains(root, "USE THIS FOLDER"):
        raise AssertionError("SAF picker is not at a selectable Pictures/DZlog folder")
    tap_any(["USE THIS FOLDER", "Use this folder", "이 폴더 사용", "Select", "선택"])
    mark("04-saf-confirm")
    if not contains(hierarchy(), "시작하기"):
        try:
            tap_any(["ALLOW", "Allow", "허용"], timeout=35)
        except AssertionError:
            pass  # Some provider variants grant without a second confirmation.
    root = wait_for("시작하기", timeout=90)
    if contains(root, "필요한 설정을 완료해 주세요"):
        raise AssertionError("Permission setup not completed after selecting exact DZlog folder")
    mark("05-first-onboarding")
    print("PASS integrated required SAF folder -> first onboarding", flush=True)


def navigate_onboarding():
    # Reuse already verified independent Android16 journey, now in the integrated app.
    from android16_journey_smoke import main as onboarding_journey_main
    onboarding_journey_main()
    mark("06-onboarding-home-camera-settings")
    print("PASS integrated onboarding and camera navigation", flush=True)


def navigate_gallery():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY, timeout=70)
    root = wait_for("촬영 시작", timeout=70)
    if contains(root, "시작하기"):
        raise AssertionError("Onboarding repeated after 5 real captures")
    tap("앨범 보기 >", "갤러리")
    root = wait_for("갤러리", timeout=70)
    if not contains(root, "최근 촬영"):
        raise AssertionError("Recent captures entry missing from integrated gallery")
    mark("07-gallery-with-saved-media")
    check_both(query_media(), 5)
    print("PASS integrated gallery navigation and saved BOTH media intact", flush=True)


def backup_roundtrip():
    # Existing dedicated SAF export/import test, with strict import requirement.
    open_settings()
    export_backup()
    imported = import_backup()
    mark("08-backup-after-import-attempt")
    if not imported:
        raise AssertionError("SAF export succeeded but actual import was not verified")
    if not alive():
        raise AssertionError("App process stopped after backup import")
    print("PASS integrated .dzlog SAF export and non-destructive import", flush=True)


def main():
    install_once_ready()
    link_exact_folder()
    navigate_onboarding()
    capture_media_main()
    mark("06b-both-five-saved")
    navigate_gallery()
    backup_roundtrip()
    print("PASS POSTRELEASE FULL JOURNEY: permissions, SAF, onboarding, capture, gallery, backup", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            mark("FAILED-final-state")
            (EVIDENCE / "window.txt").write_text(
                adb("shell", "dumpsys", "window", timeout=45), encoding="utf-8"
            )
            (EVIDENCE / "app-logcat.txt").write_text(
                adb("logcat", "-d", "-t", "3500", timeout=45), encoding="utf-8"
            )
        except Exception:
            pass
        raise
