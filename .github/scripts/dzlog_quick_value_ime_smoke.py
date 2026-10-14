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
    before = evidence("quick-edit-01-before-keyboard")
    if not contains(before, "파일명") or not contains(before, "저장경로"):
        raise AssertionError("Filename and save-path previews absent before keyboard")
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
    window = adb("shell", "dumpsys", "window", timeout=45)
    native_tops = [int(top) for top in re.findall(
        r"type=ime frame=\[\d+,(\d+)\]\[\d+,\d+\][^\n]*visible=true", window)]
    keyboard_tops = ime_rows + native_tops
    if not keyboard_tops:
        raise AssertionError("Cannot verify keyboard bounds; do not pass visibility heuristically")
    keyboard_top = min(keyboard_tops)
    if y2 > keyboard_top + 8:
        raise AssertionError(
            f"Quick-value editor overlaps keyboard: editor bottom {y2}, keyboard top {keyboard_top}"
        )
    if not contains(root, "빠른 수정") or not contains(root, "입력"):
        raise AssertionError("Quick-value editor controls not accessible above keyboard")
    for label in ("파일명", "저장경로", "템플릿"):
        visible = [bounds(node) for node in nodes(root)
                   if (node.get("text") or "").strip().startswith(label) and bounds(node)]
        if not visible:
            raise AssertionError(f"{label} preview missing while keyboard is open")
        if not any(rect[2] > rect[0] and rect[3] > rect[1]
                   and rect[3] <= keyboard_top + 8 for rect in visible):
            raise AssertionError(f"{label} preview moved behind the keyboard")
    buttons = [bounds(node) for node in nodes(root)
               if node.get("text") == "입력" and bounds(node)]
    if not any(rect[2] > rect[0] and rect[3] > rect[1]
               and rect[3] <= keyboard_top + 8 for rect in buttons):
        raise AssertionError("Input button is clipped or behind the keyboard")
    # The explicit input action clears focus and hides only the IME. A system
    # Back may dismiss the Dialog as well; verify that navigation separately.
    tap("입력")
    time.sleep(2)
    wait_for("빠른 수정")
    after = evidence("quick-edit-03-keyboard-closed")
    if keyboard_is_visible(after):
        raise AssertionError("Input action did not close the keyboard")
    if not contains(after, "파일명") or not contains(after, "저장경로"):
        raise AssertionError("Filename/save-path previews missing after keyboard closes")
    if not contains(after, "템플릿"):
        raise AssertionError("Table preview missing after keyboard closes")
    tap("입력창 확대", "입력창 축소")
    evidence("quick-edit-04-input-expanded")
    tap("입력창 축소", "입력창 확대")
    tap("표 확대", "표 전체 보기")
    evidence("quick-edit-05-table-expanded")
    tap("표 전체 보기", "표 확대")
    tap("완료")
    wait_for("빠른 값 변경", timeout=35)
    adb("shell", "input", "keyevent", "4")
    wait_for("촬영 시작", timeout=45)
    assert alive()
    print("PASS integrated camera quick-value dialog: input visible with IME, dismiss, return", flush=True)


if __name__ == "__main__":
    try:
        import sys
        if "--standalone" in sys.argv:
            from dzlog_postrelease_full_journey import (
                install_once_ready, link_exact_folder, navigate_onboarding,
            )
            install_once_ready()
            link_exact_folder()
            navigate_onboarding()
        main()
    except Exception:
        evidence("FAILED-quick-edit")
        ime_evidence("FAILED-quick-edit")
        raise
