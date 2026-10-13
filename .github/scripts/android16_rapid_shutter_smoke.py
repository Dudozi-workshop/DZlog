#!/usr/bin/env python3
"""Rapid shutter regression for a fresh disposable Android 16 CI emulator.

The existing suite creates five BOTH photos, then exports/imports one template.
This adds a short shutter burst and verifies only committed paired JPEG files.
It must never run on a user's device or erase existing media.
"""
import re
import time

from android16_journey_smoke import ACTIVITY, PKG, adb, alive, evidence, hierarchy, tap, target, wait_for
from android16_capture_media_smoke import check_both, query_media, save_diagnostics


def restart_camera():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작")
    tap("촬영 시작", "●")
    time.sleep(7)
    evidence("25-rapid-shutter-before")


def wait_for_stable_media(base_shots, deadline_seconds=85):
    deadline = time.monotonic() + deadline_seconds
    last_count = -1
    stable = 0
    while time.monotonic() < deadline:
        rows = query_media()
        count = len(rows)
        ready = count >= (base_shots + 1) * 2 and count % 2 == 0
        if ready and all(row.get("is_pending") == "0" for row in rows):
            if count == last_count:
                stable += 1
            else:
                stable = 0
            if stable >= 3:
                return rows
        else:
            stable = 0
        last_count = count
        time.sleep(3)
    raise AssertionError("Rapid shutter failed to settle with committed photo pairs")


def main():
    restart_camera()
    original = query_media()
    if len(original) != 10:
        raise AssertionError(
            "Rapid shutter test requires exactly five known CI captures, "
            "refuses to run on other storage"
        )
    check_both(original, 5)
    x, y = target(hierarchy(), "●")

    # Fast physical tapping must not bypass the capture-in-flight guard.
    # Depending on the emulator's camera latency, one or more shots may succeed.
    script = f"i=0; while [ $i -lt 8 ]; do input tap {x} {y}; i=$((i+1)); done"
    adb("shell", "sh", "-c", script, timeout=75)
    time.sleep(6)
    after_burst = wait_for_stable_media(5)
    burst_count = len(after_burst) // 2 - 5
    if burst_count < 1 or burst_count > 8:
        raise AssertionError(f"Unexpected committed burst count: {burst_count}")
    check_both(after_burst, 5 + burst_count)
    evidence("26-rapid-shutter-burst-committed")

    # A deliberate capture after the burst must still work: gate is released.
    tap("●")
    expected = (5 + burst_count + 1) * 2
    deadline = time.monotonic() + 85
    while time.monotonic() < deadline:
        rows = query_media()
        if len(rows) == expected and all(r.get("is_pending") == "0" for r in rows):
            check_both(rows, expected // 2)
            evidence("27-rapid-shutter-followup-committed")
            if not alive():
                raise AssertionError("Camera process died after shutter burst")
            print(
                f"PASS Android16 RAPID SHUTTER: {burst_count} burst shot(s), "
                f"one followup, {len(rows)} valid JPEGs and sequential counters",
                flush=True
            )
            return
        if len(rows) > expected:
            raise AssertionError(f"Unexpected extra photos after burst: {len(rows)}")
        time.sleep(3)
    raise AssertionError("Shutter gate did not recover for followup photo")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            save_diagnostics("FAILED-rapid-shutter")
        finally:
            raise
