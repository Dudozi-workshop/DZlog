#!/usr/bin/env python3
"""Disposable API-36 developer-baseline -> approved integrated DZlog upgrade QA.

Builds are signed together by one CI runner. This is NOT a test of the
Google Play signed production APK, and it MUST NOT run on anyone's device.
"""
from pathlib import Path
import json
import subprocess
import time

from android16_journey_smoke import (
    ACTIVITY, PKG, adb, alive, contains, evidence, hierarchy, nodes, tap,
    target, wait_for,
)
from android16_capture_media_smoke import check_both, query_media, rows_after
from dzlog_onboarding_ui_visual_smoke import tap_visible
from android16_backup_saf_smoke import picker

BASELINE_APK = Path("qa-output/DZlog-pre-onboarding.apk")
CANDIDATE_APK = Path("qa-output/DZlog-integrated-candidate.apk")


def shell_bytes(*args):
    return subprocess.run(
        ["adb", "exec-out", "run-as", PKG, *args],
        capture_output=True, check=True, timeout=60,
    ).stdout


def _varint(data, pos):
    value = 0
    for i in range(10):
        if pos >= len(data):
            raise AssertionError("Truncated preferences protobuf")
        byte = data[pos]
        pos += 1
        value |= (byte & 127) << (7 * i)
        if not byte & 128:
            return value, pos
    raise AssertionError("Oversized protobuf integer")


def _fields(data):
    pos = 0
    while pos < len(data):
        tag, pos = _varint(data, pos)
        wire = tag & 7
        if wire == 0:
            value, pos = _varint(data, pos)
        elif wire == 2:
            size, pos = _varint(data, pos)
            if pos + size > len(data):
                raise AssertionError("Truncated protobuf value")
            value = data[pos:pos + size]
            pos += size
        elif wire == 1:
            value, pos = data[pos:pos + 8], pos + 8
        elif wire == 5:
            value, pos = data[pos:pos + 4], pos + 4
        else:
            raise AssertionError(f"Unsupported preference wire {wire}")
        if pos > len(data):
            raise AssertionError("Truncated protobuf field")
        yield tag >> 3, wire, value


def existing_preferences():
    blob = shell_bytes("cat", "files/datastore/dzlog_prefs.preferences_pb")
    prefs = {}
    for field, wire, entry in _fields(blob):
        if field != 1 or wire != 2:
            continue
        fields = dict((n, value) for n, w, value in _fields(entry))
        key = fields.get(1, b"").decode("utf-8")
        val = fields.get(2)
        if key and val is not None:
            options = list(_fields(val))
            prefs[key] = options
    return prefs


def string_pref(prefs, key):
    values = prefs.get(key)
    if not values:
        raise AssertionError(f"Missing stored setting {key}")
    for n, w, value in values:
        if n == 5 and w == 2:
            return value.decode("utf-8")
    raise AssertionError(f"Expected string preference for {key}: {values}")


def click_visible(label):
    root = hierarchy()
    if not contains(root, label):
        raise AssertionError(f"Cannot find {label} on old build")
    tap(label)


def legacy_permission_folder():
    """Old integration version uses a permission dialog before its intro."""
    wait_for("필요한 설정을 완료해 주세요", timeout=70)
    click_visible("DZlog 폴더 연결")
    wait_for("DZlog 폴더를 연결해 주세요", timeout=35)
    click_visible("폴더 선택 화면으로")
    picker(timeout=75)

    # Some emulator images start the DocumentsUI picker at device root.
    for label in ("Pictures", "DZlog"):
        for _ in range(25):
            root = hierarchy()
            if contains(root, label):
                break
            time.sleep(1)
        if not contains(root, label):
            raise AssertionError(f"Legacy SAF picker missing {label}")
        tap_visible(label)
        picker(timeout=45)
    for _ in range(30):
        root = hierarchy()
        labels = ("USE THIS FOLDER", "Use this folder", "이 폴더 사용")
        if any(contains(root, label) for label in labels):
            tap_visible(next(label for label in labels if contains(root, label)))
            break
        time.sleep(1)
    else:
        raise AssertionError("Legacy SAF folder selection action missing")

    affirmative = {"allow", "허용", "액세스 허용", "접근 허용"}
    for _ in range(50):
        root = hierarchy()
        if contains(root, "시작하기") and not contains(root, "DZlog 폴더를 연결해 주세요"):
            return
        for node in nodes(root):
            txt = (node.get("text") or node.get("content-desc") or "").strip()
            if txt.casefold() in affirmative:
                x, y = target(root, txt)
                adb("shell", "input", "tap", str(x), str(y))
                time.sleep(2)
                break
        else:
            time.sleep(1)
    raise AssertionError("Legacy intro did not open after folder grant")


def install(apk, replace=False):
    if not apk.exists():
        raise AssertionError(f"Required QA APK is missing: {apk}")
    args = ["adb", "install"]
    if replace:
        args.append("-r")
    args.append(str(apk))
    cmd = subprocess.run(args, text=True, capture_output=True, timeout=180)
    if cmd.returncode or "Success" not in cmd.stdout:
        raise AssertionError(f"Test install failed {apk}: {cmd.stdout} {cmd.stderr[-500:]}")


def start_home():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY, timeout=90)
    root = wait_for("촬영 시작", timeout=80)
    if contains(root, "찍고, 기록하고,") or contains(root, "어떤 기록을 시작할까요?"):
        raise AssertionError("Existing user was sent to first-install onboarding")
    alive()
    return root


def photo_metadata(rows):
    return sorted((
        r["_id"], r["_display_name"], r["relative_path"],
        r["_size"], r["mime_type"], r["is_pending"],
    ) for r in rows)


def main():
    assert adb("shell", "getprop", "ro.build.version.sdk").strip() == "36"
    if adb("shell", "pm", "list", "packages", PKG).strip():
        raise AssertionError("Refusing migration test on any device with installed DZlog")
    if query_media():
        raise AssertionError("Refusing migration test on a device containing DZlog photos")
    adb("shell", "mkdir", "-p", "/sdcard/Pictures/DZlog")
    adb("shell", "wm", "size", "720x1600")
    adb("shell", "wm", "density", "360")
    adb("shell", "settings", "put", "system", "font_scale", "1.0")

    install(BASELINE_APK)
    for permission in ("android.permission.CAMERA", "android.permission.READ_MEDIA_IMAGES"):
        adb("shell", "pm", "grant", PKG, permission)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY, timeout=90)
    legacy_permission_folder()
    evidence("legacy-01-pre-onboarding-intro")
    click_visible("건너뛰기")
    wait_for("촬영 시작", timeout=80)
    evidence("legacy-02-existing-home")

    from android16_capture_media_smoke import main as capture_baseline
    capture_baseline()  # 5 actual shots -> 10 test JPEGs on old build
    before = query_media()
    check_both(before, 5)
    images_before = photo_metadata(before)
    prefs_before = existing_preferences()
    catalog_before = string_pref(prefs_before, "table_templates_json_v1")
    active_before = string_pref(prefs_before, "active_table_template_id_v1")
    catalog_json = json.loads(catalog_before)
    if not catalog_json or not active_before:
        raise AssertionError("Legacy user catalog not stored")
    evidence("legacy-03-populated-gallery")

    install(CANDIDATE_APK, replace=True)
    start_home()
    evidence("upgrade-01-home-no-onboarding")
    prefs_after = existing_preferences()
    if catalog_before != string_pref(prefs_after, "table_templates_json_v1"):
        raise AssertionError("User's saved template catalog changed during upgrade")
    if active_before != string_pref(prefs_after, "active_table_template_id_v1"):
        raise AssertionError("User's active template changed during upgrade")
    after = query_media()
    check_both(after, 5)
    if photo_metadata(after) != images_before:
        raise AssertionError("Original/result JPEG files changed during upgrade")
    evidence("upgrade-02-existing-catalog-media-retained")
    print("PASS: old->new installed update preserves templates and 10 JPEGs", flush=True)

    tap("촬영 시작", "●")
    time.sleep(9)
    tap("●")
    updated = rows_after(12, max_seconds=120)
    check_both(updated, 6)
    if photo_metadata(updated)[:0]:
        raise AssertionError("Unexpected unreachable state")
    evidence("upgrade-03-next-capture")
    print("PASS: old build 5 shots + new build shot 6 and continuous counter", flush=True)
    print("PASS: non-destructive developer baseline -> approved integrated candidate upgrade", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            evidence("FAILED-legacy-upgrade")
        except Exception:
            pass
        raise
