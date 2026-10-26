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

def scroll_to_folder(folder_name, max_scrolls=14):
    """Navigate a DocumentsUI folder by its exact visible label."""
    for attempt in range(max_scrolls + 1):
        root = picker(timeout=30)
        matched = [
            n for n in nodes(root) if (n.get("text") or "").strip() == folder_name
        ]
        if matched:
            ui.tap(folder_name)
            return picker(timeout=35)
        if attempt < max_scrolls:
            ui.adb("shell", "input", "swipe", "570", "1860", "570", "760", "400")
            time.sleep(1)
    raise AssertionError(f"Folder {folder_name!r} not found after scrolling")

def choose_saf_folder():
    tap_any(["DZlog 폴더 연결", "폴더 연결", "연결하기"], timeout=45)
    root = picker(timeout=80)
    checkpoint("05-folder-picker")
    # Initial URI is advisory; Android DocumentsUI often opens device root
    # instead of the intended Pictures/DZlog tree. Never select the root.
    if contains(root, "Can’t use this folder") or contains(root, "Can't use this folder"):
        root = scroll_to_folder("Pictures")
        checkpoint("05b-picker-pictures")
    elif not any((n.get("text") or "").strip() == "Pictures" for n in nodes(root)):
        # The picker may already be in Pictures/DZlog, or may be in Pictures.
        headings = [(n.get("text") or "").strip() for n in nodes(root)]
        if "DZlog" not in headings:
            root = scroll_to_folder("Pictures")
            checkpoint("05b-picker-pictures")
    if not any((n.get("text") or "").strip() == "DZlog" for n in nodes(root)) or any(
        (n.get("text") or "").strip() == "Pictures" for n in nodes(root)
    ):
        root = scroll_to_folder("DZlog")
        checkpoint("05c-picker-exact-dzlog")
    else:
        # A directory listing containing 'DZlog' does not prove that DZlog
        # itself is selected; always enter the child when the header differs.
        titles = [(n.get("text") or "").strip() for n in nodes(root)]
        if titles.count("DZlog") == 1 and "Pictures" in titles:
            root = scroll_to_folder("DZlog")
            checkpoint("05c-picker-exact-dzlog")
    # At exact directory the picker title must name DZlog (not device root).
    root = picker(timeout=30)
    titles = [(n.get("text") or "").strip() for n in nodes(root)]
    if "DZlog" not in titles or any("use this folder" in t.lower() and "can't" in t.lower() for t in titles):
        checkpoint("05d-invalid-folder")
        raise AssertionError("SAF picker is not at the exact DZlog folder")
    if any("Can’t use this folder" in t or "Can't use this folder" in t for t in titles):
        checkpoint("05d-folder-blocked")
        raise AssertionError("SAF provider does not allow selected directory")
    if not (contains(root,"USE THIS FOLDER") or contains(root,"이 폴더 사용")):
        raise AssertionError("The exact DZlog folder has no permission action")
    tap_any(["USE THIS FOLDER", "Use this folder", "이 폴더 사용"])
    root = ui.hierarchy()
    if any(contains(root,n) for n in ["ALLOW", "Allow", "허용"]):
        tap_any(["ALLOW", "Allow", "허용"])
    root = ui.hierarchy()
    checkpoint("05e-folder-link-return")
    return root

def main():
    ui.adb("shell","mkdir","-p","/sdcard/Pictures/DZlog")
    # Simulate an existing production DZlog photo on the same device BEFORE
    # installing the isolated QA package. Shared media must not suppress
    # this new package's starter selection, and must never be deleted.
    fixture = "app/src/main/res/drawable-nodpi/onboarding_gallery_photo.jpg"
    device_photo = "/sdcard/Pictures/DZlog/pr94-preexisting-photo.jpg"
    ui.adb("push", fixture, device_photo, timeout=50)
    ui.adb("shell","am","broadcast","-a","android.intent.action.MEDIA_SCANNER_SCAN_FILE",
           "-d","file://"+device_photo,timeout=50)
    ui.adb("shell","test","-s",device_photo)
    # Prove the shared file is actually indexed in MediaStore before launch.
    from android16_capture_media_smoke import query_media
    indexed = False
    for _ in range(12):
        if any(item.get("_display_name") == "pr94-preexisting-photo.jpg"
               for item in query_media()):
            indexed = True
            break
        time.sleep(2)
    if not indexed:
        raise AssertionError("Pre-existing DZlog photo not indexed in MediaStore")
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
    ui.adb("shell","test","-s",device_photo)  # never delete or move real shared media
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
