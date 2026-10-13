#!/usr/bin/env python3
"""Android 16 backup SAF export/import verification on disposable CI emulator.

Runs after real capture smoke; never on actual user storage.
"""
import re
import time

from android16_journey_smoke import (
    ACTIVITY, OUT, PKG, adb, alive, contains, evidence, hierarchy,
    nodes, tap, target, wait_for,
)


def picker(timeout=50):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        root = hierarchy()
        if any(
            "documentsui" in (node.get("package") or "").lower()
            for node in nodes(root)
        ):
            return root
        time.sleep(1.5)
    raise AssertionError("Android SAF document picker did not appear")


def choose_picker_save():
    root = picker()
    evidence("19-backup-saf-create-picker")
    # DocumentsUI can be English even when app strings are Korean.
    for label in ("SAVE", "Save", "저장", "확인"):
        try:
            x, y = target(root, label)
        except AssertionError:
            continue
        adb("shell", "input", "tap", str(x), str(y))
        return
    raise AssertionError("SAF create-document picker has no visible Save action")


def open_settings():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작")
    tap("설정", "작업 환경 내보내기")


def export_backup():
    tap("작업 환경 내보내기", "저장 위치 선택")
    evidence("20-backup-export-ready")
    tap("저장 위치 선택")
    choose_picker_save()
    wait_for("백업 파일을 저장했습니다", timeout=70)
    evidence("21-backup-export-success")


def import_backup():
    adb("shell", "input", "keyevent", "4")
    wait_for("작업 환경 가져오기")
    tap("작업 환경 가져오기", "다른 백업 파일 선택")
    evidence("22-backup-import-ready")
    root = hierarchy()
    if not contains(root, "이 백업 가져오기"):
        # Some document providers decline a persistent READ grant from CreateDocument.
        # The file remains exported; the user can always select it manually.
        tap("모든 파일에서 찾기")
        picker()
        evidence("23-backup-saf-open-picker")
        adb("shell", "input", "keyevent", "4")
        wait_for("다른 백업 파일 선택")
        print("PARTIAL Android16 backup SAF: exported; recent-URI read grant unavailable", flush=True)
        return False
    tap("이 백업 가져오기")
    wait_for("이름 중복", timeout=70)
    root = evidence("23-backup-import-preview")
    if not contains(root, "1개 유지") or not contains(root, "1개 추가"):
        raise AssertionError("Backup import preview did not preserve one existing template")
    tap("가져오기")
    wait_for("1개 템플릿을 추가했습니다", timeout=70)
    evidence("24-backup-import-success")
    if not alive():
        raise AssertionError("App process died during SAF export/import")
    return True


def main():
    open_settings()
    export_backup()
    imported = import_backup()
    print(
        "PASS Android16 backup SAF: exported, "
        + ("imported duplicate non-destructively" if imported else "picker access verified; import requires file selection"),
        flush=True,
    )


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            evidence("FAILED-backup-saf")
            (OUT / "FAILED-backup-saf-window.txt").write_text(
                adb("shell", "dumpsys", "window", timeout=50), encoding="utf-8"
            )
        except Exception:
            pass
        raise
