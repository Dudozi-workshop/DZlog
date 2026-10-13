#!/usr/bin/env python3
"""Android 16 first-run journey smoke test; uses adb and Python stdlib only."""

import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = "com.dudoziworkshop.dzlog"
ACTIVITY = f"{PKG}/.MainActivity"
OUT = Path("api36-journey")
OUT.mkdir(exist_ok=True)
DEVICE_XML = "/sdcard/dzlog-journey.xml"
LOCAL_XML = OUT / "current.xml"


def adb(*args, timeout=45):
    completed = subprocess.run(
        ["adb", *args], capture_output=True, text=True, timeout=timeout
    )
    if completed.returncode:
        raise RuntimeError(
            f"adb {args} failed ({completed.returncode}): {completed.stderr[-1200:]}"
        )
    return completed.stdout


def hierarchy():
    for attempt in range(4):
        try:
            adb("shell", "uiautomator", "dump", DEVICE_XML, timeout=55)
            adb("pull", DEVICE_XML, str(LOCAL_XML))
            return ET.parse(LOCAL_XML).getroot()
        except (OSError, ET.ParseError, RuntimeError, subprocess.TimeoutExpired):
            if attempt == 3:
                raise
            time.sleep(1.5)


def nodes(root):
    return list(root.iter("node"))


def contains(root, name):
    return any(
        name in (node.get("text") or "") or
        name in (node.get("content-desc") or "")
        for node in nodes(root)
    )


def wait_for(label, timeout=35):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        root = hierarchy()
        if contains(root, label):
            return root
        time.sleep(1.5)
    raise AssertionError(f"Expected accessibility text not visible: {label}")


def target(root, name):
    parent = {child: node for node in root.iter() for child in node}
    matches = [
        node for node in nodes(root)
        if node.get("text") == name or node.get("content-desc") == name
    ]
    if not matches:
        raise AssertionError(f"Could not locate action: {name}")
    for node in matches:
        current = node
        while current in parent and current.get("clickable") != "true":
            current = parent[current]
        bounds = current.get("bounds") or node.get("bounds") or ""
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            if x2 > x1 and y2 > y1:
                return ((x1 + x2) // 2, (y1 + y2) // 2)
    raise AssertionError(f"Action has no usable bounds: {name}")


def tap(name, expected=None):
    x, y = target(hierarchy(), name)
    adb("shell", "input", "tap", str(x), str(y))
    if expected:
        wait_for(expected)


def evidence(name):
    (OUT / f"{name}.png").write_bytes(
        subprocess.run(
            ["adb", "exec-out", "screencap", "-p"],
            check=True, capture_output=True, timeout=40
        ).stdout
    )
    root = hierarchy()
    ET.ElementTree(root).write(OUT / f"{name}.xml", encoding="utf-8")
    return root


def alive():
    pid = adb("shell", "pidof", PKG).strip()
    if not pid:
        raise AssertionError("DZlog process not alive")
    return pid


def wait_for_home_after_onboarding():
    """The first launch may show DZlog's optional photo-access prompt."""
    deadline = time.monotonic() + 45
    while time.monotonic() < deadline:
        root = hierarchy()
        if contains(root, "나중에") and contains(root, "사진 접근 허용"):
            # Preserve the permission-denied journey; camera access is tested later.
            tap("나중에")
            continue
        if contains(root, "촬영 시작"):
            return root
        time.sleep(1.5)
    raise AssertionError("Home not ready after dismissing optional photo-access prompt")


def grant_camera_dialog_if_visible():
    root = hierarchy()
    permission_nodes = [
        node for node in nodes(root)
        if any(
            phrase in (node.get("resource-id") or "")
            for phrase in (
                "permission_allow_foreground_only_button",
                "permission_allow_one_time_button",
                "permission_allow_button",
            )
        )
    ]
    if permission_nodes:
        bounds = permission_nodes[0].get("bounds", "")
        coords = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if not coords:
            raise AssertionError("Camera permission button has no bounds")
        x1, y1, x2, y2 = map(int, coords.groups())
        adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
        return True
    for label in ("앱 사용 중에만 허용", "While using the app", "허용", "Allow"):
        if contains(root, label):
            tap(label)
            return True
    return False


def main():
    api = adb("shell", "getprop", "ro.build.version.sdk").strip()
    assert api == "36", f"Expected Android API 36; actual {api}"
    wait_for("시작하기")
    evidence("01-onboarding-intro")
    tap("시작하기", "다음")
    evidence("02-onboarding-guide")
    tap("다음", "이 양식으로 시작")
    tap("업무·현장", "다른 양식 보기")
    evidence("03-onboarding-category")
    tap("건너뛰기")
    wait_for_home_after_onboarding()
    evidence("04-home")

    tap("설정", "사진 품질")
    evidence("05-settings")
    tap("사진 품질", "속도 우선")
    evidence("06-settings-expanded")
    tap("사진 품질")
    adb("shell", "input", "keyevent", "4")
    wait_for("촬영 시작")

    tap("촬영 시작")
    time.sleep(3)
    evidence("07-camera-permission")
    granted_by_dialog = grant_camera_dialog_if_visible()
    if granted_by_dialog:
        time.sleep(3)
    camera_root = hierarchy()
    if not (contains(camera_root, "촬영 도구") or contains(camera_root, "사진 목록")):
        raise AssertionError("Camera controls absent after permission flow")
    evidence("08-camera-controls")
    alive()

    adb("shell", "input", "keyevent", "4")
    wait_for("촬영 시작")
    evidence("09-back-home")

    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작")
    root = evidence("10-relaunch-home")
    if contains(root, "시작하기"):
        raise AssertionError("Onboarding unexpectedly repeated after relaunch")
    print(f"PASS Android {api}: onboarding, settings, permission, camera, back, relaunch")
    print(f"PID: {alive()} | permission dialog handled: {granted_by_dialog}")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            evidence("FAILED-last-screen")
            (OUT / "window-dump.txt").write_text(
                adb("shell", "dumpsys", "window", timeout=50), encoding="utf-8"
            )
        except Exception:
            pass
        raise
