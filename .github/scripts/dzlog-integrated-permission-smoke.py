#!/usr/bin/env python3
"""Fresh Android 16 integrated permission-gate regression (no user-data resets)."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = "com.dudoziworkshop.dzlog"
OUT = Path("postrelease-permissions")
OUT.mkdir(exist_ok=True)
REMOTE_XML = "/sdcard/dzlog-postrelease-setup.xml"
LOCAL_XML = OUT / "current.xml"


def adb(*args, timeout=55):
    p = subprocess.run(["adb", *args], text=True, capture_output=True, timeout=timeout)
    if p.returncode:
        raise RuntimeError(f"adb {args}: {p.stderr[-1100:]}")
    return p.stdout


def install_apk():
    """Wait for a responsive package manager before installing the test APK.

    Emulators occasionally report boot-complete while package service is still busy.
    Do not mistake an adb install timeout for a DZlog permission-gate failure.
    """
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        try:
            boot = adb("shell", "getprop", "sys.boot_completed", timeout=20).strip()
            package_manager = adb("shell", "service", "check", "package", timeout=20)
            if boot == "1" and "found" in package_manager.lower():
                break
        except (subprocess.TimeoutExpired, RuntimeError):
            pass
        time.sleep(3)
    else:
        raise RuntimeError("Android emulator package manager never became ready")

    apk = "app/build/outputs/apk/debug/app-debug.apk"
    for attempt in range(2):
        try:
            result = adb("install", "-r", apk, timeout=180)
            if "Success" not in result:
                raise RuntimeError(f"Unexpected adb install output: {result[-600:]}")
            return
        except (subprocess.TimeoutExpired, RuntimeError) as error:
            if attempt:
                raise RuntimeError("APK installation failed on emulator after retry") from error
            print(f"Emulator install not ready; retry once: {error}", flush=True)
            subprocess.run(["adb", "kill-server"], timeout=30, check=False)
            subprocess.run(["adb", "start-server"], timeout=30, check=True)
            adb("wait-for-device", timeout=120)
            time.sleep(8)


def hierarchy():
    # UIAutomator can transiently fail just after a fresh Android boot.
    for attempt in range(4):
        try:
            adb("shell", "uiautomator", "dump", REMOTE_XML, timeout=75)
            adb("pull", REMOTE_XML, str(LOCAL_XML), timeout=55)
            return ET.parse(LOCAL_XML).getroot()
        except (subprocess.TimeoutExpired, RuntimeError, ET.ParseError, OSError):
            if attempt == 3:
                raise
            time.sleep(3)


def has(root, text):
    return any(text in (n.get("text") or "") or
               text in (n.get("content-desc") or "") for n in root.iter("node"))


def evidence(name, root=None):
    root = root or hierarchy()
    ET.ElementTree(root).write(OUT / f"{name}.xml", encoding="utf-8")
    (OUT / f"{name}.png").write_bytes(
        subprocess.run(["adb", "exec-out", "screencap", "-p"],
                       capture_output=True, check=True, timeout=40).stdout)


def require(label, timeout=45):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        root = hierarchy()
        if has(root, label):
            return root
        time.sleep(1.2)
    raise AssertionError(f"Expected permission setup text: {label}")


def launch():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", f"{PKG}/.MainActivity")


def assert_no_home(root):
    if has(root, "촬영 시작") or has(root, "최근 촬영"):
        raise AssertionError("Required setup gate was bypassed")


def click(label):
    root = hierarchy()
    parent = {child: n for n in root.iter() for child in n}
    for n in root.iter("node"):
        if n.get("text") != label and n.get("content-desc") != label:
            continue
        target = n
        while target in parent and target.get("clickable") != "true":
            target = parent[target]
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                             target.get("bounds") or n.get("bounds") or "")
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
            return
    raise AssertionError(f"No clickable action with label: {label}")


def main():
    assert adb("shell", "getprop", "ro.build.version.sdk").strip() == "36"
    install_apk()
    launch()
    root = require("카메라 권한 설정")
    assert_no_home(root)
    evidence("01-camera-required", root)

    adb("shell", "pm", "grant", PKG, "android.permission.CAMERA")
    launch()
    root = require("사진 접근 권한 설정")
    assert_no_home(root)
    evidence("02-full-photo-access-required", root)

    adb("shell", "pm", "grant", PKG, "android.permission.READ_MEDIA_IMAGES")
    launch()
    root = require("DZlog 폴더 연결")
    assert_no_home(root)
    evidence("03-exact-folder-required", root)

    click("DZlog 폴더 연결")
    root = require("DZlog 폴더를 연결해 주세요")
    assert_no_home(root)
    if has(root, "취소"):
        raise AssertionError("Required folder guidance exposed optional Cancel")
    evidence("04-required-folder-guide", root)
    assert adb("shell", "pidof", PKG).strip()
    print("PASS API36 integration: camera -> full images -> exact DZlog folder gate; no bypass")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            evidence("FAILED-last-screen")
            (OUT / "window-dump.txt").write_text(adb("shell", "dumpsys", "window"))
        except Exception:
            pass
        raise
