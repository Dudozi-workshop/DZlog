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

PKG = "com.dudoziworkshop.dzlog.qa.pr94"
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
    fatal=[line for line in log.splitlines() if "FATAL EXCEPTION" in line or
           ("Process: " + PKG) in line or "AndroidRuntime" in line and "Exception" in line]
    return fatal[:25]

def assert_alive(where):
    if not ui.adb("shell","pidof",PKG).strip():
        raise AssertionError("Process died at "+where)
    fatal=save_crashlog()
    if fatal:
        raise AssertionError("AndroidRuntime error at "+where+": "+str(fatal))

def choose_saf_folder():
    tap_any(["DZlog 폴더 연결", "폴더 연결", "연결하기"], timeout=45)
    root = picker(timeout=80)
    checkpoint("05-folder-picker")
    if not contains(root, "DZlog"):
        if contains(root,"Pictures"):
            tap_any(["Pictures"])
            root=picker()
        else:
            raise AssertionError("Pictures/DZlog not visible in SAF picker")
    if not contains(root, "USE THIS FOLDER"):
        tap_any(["DZlog"])
        root=picker(timeout=40)
    if not (contains(root,"USE THIS FOLDER") or contains(root,"이 폴더 사용")):
        raise AssertionError("SAF tree not selectable")
    tap_any(["USE THIS FOLDER","Use this folder","이 폴더 사용"])
    root=ui.hierarchy()
    if any(contains(root,n) for n in ["ALLOW","Allow","허용"]):
        tap_any(["ALLOW","Allow","허용"])
    return root

def main():
    ui.adb("shell","mkdir","-p","/sdcard/Pictures/DZlog")
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
    ui.wait_for("DZlog 폴더 연결",timeout=50)
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
    ui.wait_for("촬영 시작",timeout=65)
    checkpoint("08-home")
    assert_alive("home")
    print("PASS PR94 Android16 actual first-run setup, template choice, home",flush=True)

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
