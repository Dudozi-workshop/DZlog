#!/usr/bin/env python3
"""PR #94 startup runtime smoke; disposable Android 16 CI emulator only.

Checks actual process lifetime and first-run onboarding, never changes user photos.
"""
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, ".github/scripts")
import android16_journey_smoke as ui

PKG = "com.dudoziworkshop.dzlog.qa.pr94fix"
ui.PKG = PKG
ui.ACTIVITY = PKG + "/com.dudoziworkshop.dzlog.MainActivity"
from dzlog_postrelease_full_journey import tap_any
from android16_backup_saf_smoke import picker
from android16_journey_smoke import nodes, contains

OUT = Path("pr94-runtime-evidence")
OUT.mkdir(exist_ok=True)

def checkpoint(name):
    root = ui.hierarchy()
    import xml.etree.ElementTree as ET
    ET.ElementTree(root).write(OUT / (name + ".xml"), encoding="utf-8")
    (OUT / (name + ".png")).write_bytes(
        subprocess.run(["adb","exec-out","screencap","-p"],
                       check=True,capture_output=True,timeout=45).stdout)
    return root

def save_crashlog():
    log=subprocess.run(["adb","logcat","-d","-t","10000"],
                       capture_output=True,text=True,timeout=80).stdout
    (OUT / "logcat.txt").write_text(log,encoding="utf-8")
    # Android 16 Google APIs emulators may crash a Play Services process.
    # Only the tested DZlog package is a startup failure for this workflow.
    lines = log.splitlines()
    fatal = []
    for i, line in enumerate(lines):
        if "FATAL EXCEPTION" not in line:
            continue
        context = lines[i:i+5]
        if any(("Process: " + PKG + ",") in candidate for candidate in context):
            fatal.extend(context)
    return fatal[:25]

def assert_alive(where):
    if not ui.adb("shell","pidof",PKG).strip():
        raise AssertionError("Process died at "+where)
    fatal=save_crashlog()
    if fatal:
        raise AssertionError("AndroidRuntime error at "+where+": "+str(fatal))

def enter_directory(name):
    """Use the current DocumentsUI hierarchy, not a second stale dump."""
    for attempt in range(14):
        root = picker(timeout=40)
        entries = [n for n in nodes(root) if (n.get("text") or "").strip() == name]
        if entries:
            x, y = ui.target(root, name)
            ui.adb("shell", "input", "tap", str(x), str(y))
            return picker(timeout=40)
        ui.adb("shell", "input", "swipe", "570", "1860", "570", "760", "400")
        time.sleep(1)
    raise AssertionError(f"Folder {name!r} not visible in the picker")

def choose_saf_folder():
    tap_any(["Pictures 폴더 선택하기", "연결하기", "폴더 연결", "DZlog 폴더 연결"], timeout=45)
    root = picker(timeout=80)
    checkpoint("05-folder-picker")
    # Latest integration onboarding asks for Pictures, and DZlog is created
    # inside it by GalleryFolderStorage. Do not require a preexisting DZlog.
    def is_pictures_directory(root):
        return any((n.get("resource-id") or "").endswith(":id/breadcrumb_text")
                   and (n.get("text") or "").strip() == "Pictures" for n in nodes(root))
    if not is_pictures_directory(root):
        root = enter_directory("Pictures")
    checkpoint("05b-picker-pictures")
    if not is_pictures_directory(root):
        raise AssertionError("DocumentsUI did not navigate into Pictures")
    if not (contains(root, "USE THIS FOLDER") or contains(root, "이 폴더 사용")):
        raise AssertionError("Pictures folder is not selectable")
    tap_any(["USE THIS FOLDER", "Use this folder", "이 폴더 사용"])
    root = ui.hierarchy()
    if any(contains(root, n) for n in ["ALLOW", "Allow", "허용"]):
        tap_any(["ALLOW", "Allow", "허용"])
    checkpoint("05c-pictures-linked")
    return ui.hierarchy()

def main():
    # Fresh device: the app must provision Pictures/DZlog after the user
    # selects Pictures. Do not create the child folder in the test setup.
    ui.adb("shell","mkdir","-p","/sdcard/Pictures")
    ui.adb("install","-r","app/build/outputs/apk/debug/app-debug.apk",timeout=170)
    for p in ("android.permission.CAMERA","android.permission.READ_MEDIA_IMAGES"):
        ui.adb("shell","pm","grant",PKG,p)
    ui.adb("logcat","-c")
    ui.adb("shell","am","start","-W","-n",ui.ACTIVITY,timeout=60)
    ui.wait_for("시작하기",timeout=75)
    checkpoint("01-first-launch")
    assert_alive("introduction")
    ui.tap("시작하기", "사용 준비하기")
    checkpoint("02-intro-features")
    ui.tap("사용 준비하기")
    ui.wait_for("Pictures 폴더",timeout=50)
    checkpoint("03-permission-setup")
    assert_alive("permission setup")
    choose_saf_folder()
    try:
        ui.wait_for("어떤 기록을",timeout=75)
    except Exception:
        checkpoint("06-MISSING-template-screen")
        raise
    checkpoint("07-template-choice")
    assert_alive("starter selection")
    ui.tap("DZlog 시작하기")
    # An initial tutorial invitation may cover the Home button; dismiss the
    # optional prompt without disabling or altering any actual user feature.
    invite_deadline = time.monotonic() + 65
    while time.monotonic() < invite_deadline:
        current = ui.hierarchy()
        if contains(current, "DZlog 사용법을") and contains(current, "나중에"):
            checkpoint("08-tutorial-invitation")
            ui.tap("나중에")
            break
        if contains(current, "촬영 시작"):
            break
        time.sleep(1.5)
    ui.wait_for("촬영 시작",timeout=50)
    checkpoint("09-home")
    assert_alive("home")
    # Confirm the selected basic starter survives Activity/process restart.
    ui.adb("shell","am","force-stop",PKG)
    ui.adb("shell","am","start","-W","-n",ui.ACTIVITY,timeout=65)
    ui.wait_for("촬영 시작",timeout=65)
    checkpoint("10-home-after-relaunch")
    assert_alive("relaunch with saved starter")
    print("PASS PR94 Android16 first-run permission, starter, home and relaunch",flush=True)

if __name__=="__main__":
    try:
        main()
    finally:
        try:
            fatal=save_crashlog()
            print("FATAL LOGS:", fatal,flush=True)
            checkpoint("final")
        except Exception as ex:
            print("Evidence collection error",str(ex),flush=True)
