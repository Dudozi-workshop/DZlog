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


def hierarchy():
    adb("shell", "uiautomator", "dump", REMOTE_XML)
    adb("pull", REMOTE_XML, str(LOCAL_XML))
    return ET.parse(LOCAL_XML).getroot()


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
    adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk")
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
