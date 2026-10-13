#!/usr/bin/env python3
"""Android 16 table-editor keyboard regression smoke, after first-run journey."""

import re
import time
from pathlib import Path

from android16_journey_smoke import (
    OUT,
    adb,
    alive,
    contains,
    evidence,
    hierarchy,
    nodes,
    tap,
    wait_for,
)


def ime_evidence(label):
    (OUT / f"{label}-input-method.txt").write_text(
        adb("shell", "dumpsys", "input_method", timeout=50),
        encoding="utf-8",
    )
    (OUT / f"{label}-window.txt").write_text(
        adb("shell", "dumpsys", "window", timeout=50),
        encoding="utf-8",
    )


def keyboard_is_visible(root):
    # Prefer real IME hierarchy over heuristic focus/animation indicators.
    other_packages = {
        node.get("package") or ""
        for node in nodes(root)
        if node.get("package") and node.get("package") != "com.dudoziworkshop.dzlog"
    }
    ime_packages = [
        pkg for pkg in other_packages
        if any(part in pkg.lower() for part in ("inputmethod", "keyboard", "latinime"))
    ]
    if ime_packages:
        return True

    state = adb("shell", "dumpsys", "input_method", timeout=50)
    return bool(
        re.search(
            r"(?:mInputShown|mIsInputViewShown|isInputViewShown)\s*=\s*true",
            state,
        )
    )


def main():
    wait_for("촬영 시작")
    tap("표 편집")
    wait_for("셀을 선택해 내용을 편집하세요.")
    evidence("11-editor-before-cell")

    # Fresh starter template: the visible header cell is known to be text.
    tap("기록명")
    wait_for("값")
    evidence("12-editor-cell-selected")

    tap("값")
    time.sleep(2)
    active_root = evidence("13-editor-ime-open")
    ime_evidence("13-editor-ime-open")

    if not keyboard_is_visible(active_root):
        raise AssertionError("Editor text field did not open a visible software keyboard")
    if not contains(active_root, "값"):
        raise AssertionError("Active cell input is absent while software keyboard is shown")
    if contains(active_root, "저장설정"):
        raise AssertionError("Bottom navigation still visible while editor IME is open")

    adb("shell", "input", "keyevent", "4")
    wait_for("저장설정")
    evidence("14-editor-ime-closed")

    if not alive():
        raise AssertionError("App process died while opening/closing editor keyboard")
    print("PASS Android 16 editor: cell selection, IME open, bottom-nav transition, IME close")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            evidence("FAILED-editor-ime")
            ime_evidence("FAILED-editor-ime")
        except Exception:
            pass
        raise
