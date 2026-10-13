#!/usr/bin/env python3
"""Android 16 real-shutter + MediaStore BOTH regression on a fresh CI emulator.

Never run this against a user's phone: it creates five test captures.
The workflow launches this only after its fresh-install, non-destructive QA.
"""
import collections
import re
import subprocess
import time
from pathlib import Path

from android16_journey_smoke import OUT, PKG, adb, alive, evidence, tap, wait_for

MEDIA_URI = "content://media/external/images/media"
FIELDS = "_id:_display_name:relative_path:mime_type:_size:is_pending"
ROW_FIELD = re.compile(r"(?:^|,\s+)([_a-zA-Z]+)=([\s\S]*?)(?=,\s+[_a-zA-Z]+=|$)")
BASE = "Pictures/DZlog/"


def query_media():
    raw = adb("shell", "content", "query", "--uri", MEDIA_URI,
              "--projection", FIELDS, timeout=70)
    (OUT / "capture-mediastore-query.txt").write_text(raw, encoding="utf-8")
    result = []
    for line in raw.splitlines():
        if not line.startswith("Row:"):
            continue
        fields = dict(ROW_FIELD.findall(line.split(" ", 2)[-1]))
        if fields.get("relative_path", "").startswith(BASE):
            result.append(fields)
    if ("Row:" in raw) and not result and BASE in raw:
        raise AssertionError("MediaStore returned DZlog rows but parser missed them")
    return result


def save_diagnostics(name):
    evidence(name)
    try:
        (OUT / f"{name}-media.txt").write_text(
            adb("shell", "content", "query", "--uri", MEDIA_URI,
                "--projection", FIELDS, timeout=70), encoding="utf-8")
        (OUT / f"{name}-files.txt").write_text(
            adb("shell", "find", "/sdcard/Pictures/DZlog", "-type", "f"),
            encoding="utf-8")
        (OUT / f"{name}-log.txt").write_text(
            adb("logcat", "-d", "-t", "3500"), encoding="utf-8")
    except Exception as exc:
        (OUT / f"{name}-diag-error.txt").write_text(str(exc), encoding="utf-8")


def rows_after(expected_count, max_seconds=110):
    start = time.monotonic()
    latest = []
    while time.monotonic() - start < max_seconds:
        latest = query_media()
        complete = [r for r in latest if r.get("is_pending") == "0"]
        if len(complete) == expected_count and len(latest) == expected_count:
            return complete
        if len(latest) > expected_count:
            raise AssertionError(f"Extra DZlog images: expected {expected_count}, got {len(latest)}")
        time.sleep(3)
    raise AssertionError(
        f"MediaStore capture timeout: expected {expected_count}, got "
        f"{len(latest)}; pending={[r.get('is_pending') for r in latest]}"
    )


def check_file(media):
    name = media.get("_display_name", "")
    rel = media.get("relative_path", "")
    assert name.lower().endswith(".jpg"), f"Unexpected image filename: {name!r}"
    assert rel.startswith(BASE), f"Unexpected path: {rel!r}"
    assert media.get("mime_type") == "image/jpeg", f"Unexpected MIME: {media!r}"
    assert int(media.get("_size", "0")) > 512, f"Unwritten JPEG: {media!r}"
    assert media.get("is_pending") == "0", f"Pending MediaStore item: {media!r}"
    # Test emulator has shell access to external test-media files.
    full_path = "/sdcard/" + rel + name
    data = subprocess.run(
        ["adb", "exec-out", "cat", full_path], capture_output=True,
        check=True, timeout=70
    ).stdout
    if len(data) <= 512 or not data.startswith(b"\xff\xd8\xff") or not data.endswith(b"\xff\xd9"):
        raise AssertionError(f"Invalid JPEG header/trailer: {full_path}")
    if len(data) != int(media["_size"]):
        raise AssertionError(f"JPEG file size disagrees with MediaStore: {full_path}")
    return full_path


def check_both(rows, expected_shots):
    assert len(rows) == expected_shots * 2
    by_name = collections.defaultdict(list)
    for item in rows:
        check_file(item)
        by_name[item["_display_name"]].append(item)
    if len(by_name) != expected_shots:
        raise AssertionError(f"Expected {expected_shots} distinct filename(s), got {list(by_name)}")
    numbers = []
    for name, pair in by_name.items():
        paths = sorted(m["relative_path"] for m in pair)
        if len(pair) != 2 or sum(p.rstrip("/").endswith("/original") for p in paths) != 1:
            raise AssertionError(f"BOTH pairing missing result/original for {name}: {paths}")
        number_match = re.search(r"_(\d+)(?:\.jpg)$", name, flags=re.IGNORECASE)
        if not number_match:
            raise AssertionError(f"Default starter counter absent from filename: {name}")
        numbers.append(int(number_match.group(1)))
    if sorted(numbers) != list(range(1, expected_shots + 1)):
        raise AssertionError(f"Fresh-install counter not strictly sequential: {numbers}")
    return sorted(numbers)


def capture_one(expected_shots):
    tap("●")
    rows = rows_after(expected_shots * 2)
    numbers = check_both(rows, expected_shots)
    save_diagnostics(f"capture-{expected_shots:02d}-committed")
    print(f"Capture {expected_shots}: {len(rows)} MediaStore items, counters {numbers}", flush=True)


def main():
    wait_for("촬영 시작")
    initial = query_media()
    if initial:
        raise AssertionError(
            "Capture test requires empty CI emulator storage; refuses to modify existing media"
        )
    tap("촬영 시작", "사진 목록")
    time.sleep(9)  # CameraX binding and initial counter readback.
    evidence("capture-00-ready")
    alive()
    for shot in range(1, 6):
        # Mid-session process restart checks whether persisted counter state and
        # MediaStore readback agree before the next committed shutter event.
        if shot == 4:
            adb("shell", "am", "force-stop", PKG)
            adb("shell", "am", "start", "-W", "-n", f"{PKG}/.MainActivity")
            wait_for("촬영 시작")
            tap("촬영 시작", "사진 목록")
            time.sleep(9)
            evidence("capture-03-process-restarted")
            check_both(query_media(), 3)
        capture_one(shot)
    assert alive()
    print("PASS Android 16 REAL CAPTURE: 5 shots, BOTH 10 JPEGs, MediaStore, restart, paths, counters", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            save_diagnostics("FAILED-real-capture")
        finally:
            raise
