#!/usr/bin/env python3
"""Non-destructive Android 16 permission deny/regrant and Settings-return QA.

Runs only on the ephemeral Android 16 CI emulator after capture smoke. Never
run this on a user's device: it changes Android application permissions.
"""
import re
import time

from android16_journey_smoke import (
    ACTIVITY, PKG, adb, alive, contains, evidence, hierarchy, nodes,
    tap, wait_for,
)
from android16_capture_media_smoke import check_both, query_media, save_diagnostics


def app_home():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작")


def settings_roundtrip(permission, action):
    adb("shell", "am", "start", "-W", "-a",
        "android.settings.APPLICATION_DETAILS_SETTINGS", "-d", f"package:{PKG}")
    time.sleep(2)
    adb("shell", "pm", action, PKG, permission)
    adb("shell", "input", "keyevent", "4")
    time.sleep(2)
    try:
        alive()
    except AssertionError:
        adb("shell", "am", "start", "-W", "-n", ACTIVITY)


def press_permission_deny():
    for _ in range(14):
        root = hierarchy()
        for node in nodes(root):
            rid = node.get("resource-id") or ""
            label = (node.get("text") or "").lower()
            if "permission_deny" not in rid and label not in (
                "허용 안 함", "허용하지 않음", "don't allow", "don’t allow", "deny"
            ):
                continue
            raw = node.get("bounds") or ""
            match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", raw)
            if match:
                a, b, c, d = map(int, match.groups())
                adb("shell", "input", "tap", str((a + c)//2), str((b + d)//2))
                return True
        if contains(root, "카메라 권한이 필요합니다"):
            return False
        time.sleep(1)
    raise AssertionError("Camera-denied UI or Android deny button did not appear")


def return_to_camera():
    root = hierarchy()
    if contains(root, "촬영 시작"):
        tap("촬영 시작")
    wait_for("촬영 도구", timeout=45)
    if not alive():
        raise AssertionError("App exited on return from permission Settings")


def wait_for_gallery_after_settings():
    for attempt in range(2):
        deadline = time.monotonic() + 30
        while time.monotonic() < deadline:
            alive()
            root = hierarchy()
            if contains(root, "갤러리"):
                return
            if contains(root, "촬영 시작"):
                time.sleep(2)
                tap("앨범 보기 >")
                break
            time.sleep(1)
        try:
            wait_for("갤러리", timeout=18)
            return
        except AssertionError:
            if attempt == 1:
                raise AssertionError("Gallery unreachable after returning from app settings")


def main():
    api = adb("shell", "getprop", "ro.build.version.sdk").strip()
    assert api == "36", f"Requires disposable Android 16 emulator, got {api}"
    before = query_media()
    assert len(before) >= 12 and len(before) % 2 == 0, (
        "Permission QA requires completed capture and burst smoke first")
    check_both(before, len(before)//2)
    original = sorted((r["_display_name"], r["relative_path"], r["_size"]) for r in before)

    # Camera revoked after a previously granted session. Deny the new prompt.
    adb("shell", "pm", "revoke", PKG, "android.permission.CAMERA")
    app_home()
    tap("촬영 시작")
    press_permission_deny()
    wait_for("카메라 권한이 필요합니다")
    evidence("28-camera-denied")
    settings_roundtrip("android.permission.CAMERA", "grant")
    return_to_camera()
    evidence("29-camera-regranted")

    adb("shell", "input", "keyevent", "4")
    wait_for("촬영 시작")
    tap("앨범 보기 >")
    wait_for("갤러리")
    evidence("30-gallery-without-photo-read-permission")

    # Photo-read permission is optional; both grant and revoke must preserve
    # navigation and app-created media. No photo is deleted or moved.
    settings_roundtrip("android.permission.READ_MEDIA_IMAGES", "grant")
    wait_for_gallery_after_settings()
    evidence("31-gallery-photo-read-granted")
    settings_roundtrip("android.permission.READ_MEDIA_IMAGES", "revoke")
    wait_for_gallery_after_settings()
    evidence("32-gallery-photo-read-revoked")

    after = query_media()
    check_both(after, len(before)//2)
    final = sorted((r["_display_name"], r["relative_path"], r["_size"]) for r in after)
    if final != original:
        raise AssertionError("Permission switching changed saved photo metadata")
    print("PASS Android16 permission return: camera deny/regrant, photo-read "
          "grant/revoke, gallery return, saved media preserved", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            save_diagnostics("FAILED-permission-return")
        finally:
            raise
