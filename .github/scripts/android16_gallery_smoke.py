#!/usr/bin/env python3
"""Android 16 gallery empty-state and cancel-only navigation smoke."""

from android16_journey_smoke import ACTIVITY, PKG, adb, alive, contains, evidence, tap, wait_for


def main():
    # A cold restart separates gallery coverage from the editor keyboard session.
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작")

    tap("앨범 보기 >")
    root = wait_for("갤러리")
    evidence("15-gallery-empty-root")
    if not any(
        contains(root, label) for label in
        ("하위 폴더", "저장된 사진이 없습니다.", "새 폴더")
    ):
        raise AssertionError("Gallery did not expose its folder or empty-state UI")

    tap("새 폴더")
    wait_for("폴더 이름")
    evidence("16-gallery-create-dialog")
    # Do not create a folder: this test deliberately leaves all data unchanged.
    tap("취소")
    restored = wait_for("새 폴더")
    if contains(restored, "폴더 이름"):
        raise AssertionError("Cancel did not close the create-folder dialog")
    evidence("17-gallery-dialog-cancelled")

    adb("shell", "input", "keyevent", "4")
    wait_for("촬영 시작")
    evidence("18-gallery-back-home")
    if not alive():
        raise AssertionError("App process exited during gallery navigation")

    print("PASS Android 16 gallery: empty state, new-folder dialog cancel, home back")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            evidence("FAILED-gallery")
        except Exception:
            pass
        raise
