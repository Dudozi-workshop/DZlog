#!/usr/bin/env python3
"""Quick-value Dialog IME visibility regression on disposable Android 16 emulator."""
import re
import time

from android16_journey_smoke import (
    adb, alive, contains, evidence, hierarchy, nodes, tap, wait_for,
)
from android16_editor_keyboard_smoke import keyboard_is_visible, ime_evidence


def bounds(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds") or "")
    return tuple(map(int, match.groups())) if match else None


def main():
    wait_for("촬영 시작")
    tap("촬영 시작", "●")
    wait_for("빠른 값 변경", timeout=60)
    tap("빠른 값 변경", "빠른 수정")
    evidence("quick-edit-01-before-keyboard")
    wait_for("값 입력", timeout=35)
    tap("값 입력")
    time.sleep(3)
    root = evidence("quick-edit-02-ime-open")
    ime_evidence("quick-edit-02-ime-open")
    if not keyboard_is_visible(root):
        raise AssertionError("Quick-value input did not open a visible IME")
    editors = [node for node in nodes(root)
               if "EditText" in (node.get("class") or "") and
               node.get("focused") == "true" and bounds(node)]
    if not editors:
        raise AssertionError("Focused quick-value editor missing while IME visible")
    editor = editors[0]
    x1, y1, x2, y2 = bounds(editor)
    if x2 <= x1 or y2 <= y1:
        raise AssertionError("Quick-value text editor has no visible bounds")
    ime_nodes = [
        node for node in nodes(root)
        if node.get("package") and
        any(t in node.get("package", "").lower() for t in ("inputmethod", "keyboard", "latinime"))
        and bounds(node)
    ]
    ime_rows = [bounds(node)[1] for node in ime_nodes if bounds(node)[1] > 0]
    if ime_rows:
        # Ignore very small IME decorative elements at the top of its own window.
        keyboard_top = min(ime_rows)
        if y2 > keyboard_top + 8:
            raise AssertionError(
                f"Quick-value editor overlaps keyboard: editor bottom {y2}, keyboard top {keyboard_top}"
            )
    if not contains(root, "빠른 수정") or not contains(root, "입력"):
        raise AssertionError("Quick-value editor controls not accessible above keyboard")
    adb("shell", "input", "keyevent", "4")
    time.sleep(2)
    wait_for("빠른 수정")
    evidence("quick-edit-03-keyboard-closed")
    tap("완료")
    wait_for("빠른 값 변경", timeout=35)
    adb("shell", "input", "keyevent", "4")
    wait_for("촬영 시작", timeout=45)
    assert alive()
    print("PASS integrated camera quick-value dialog: input visible with IME, dismiss, return", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        evidence("FAILED-quick-edit")
        ime_evidence("FAILED-quick-edit")
        raise
