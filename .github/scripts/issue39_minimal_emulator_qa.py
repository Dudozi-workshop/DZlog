#!/usr/bin/env python3
"""Issue #39 ONLY: disposable API36 emulator, original/result deletion and shutter interruption.

Never use on a user's phone or shared photo library. Refuse nonempty MediaStore at startup.
This verifies actual UI deletion and a best-effort interruption window, NOT injected I/O failure.
"""
import re
import os
import subprocess
import time
from pathlib import Path
from android16_journey_smoke import PKG, ACTIVITY, OUT, adb, hierarchy, nodes, tap, wait_for, evidence, target
from android16_capture_media_smoke import query_media, check_file, rows_after, save_diagnostics
from dzlog_postrelease_full_journey import install_once_ready, link_exact_folder

REPORT = Path("issue39-evidence")
REPORT.mkdir(exist_ok=True)

def check_empty():
    rows = query_media()
    if rows:
        raise AssertionError("Refusing destructive QA: emulator is not empty")

def mark(name):
    evidence("issue39-" + name)
    (REPORT / (name + ".txt")).write_text(str(query_media()), encoding="utf-8")

def bounds(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    return tuple(map(int, match.groups())) if match else None

def finish_setup():
    wait_for("시작하기", timeout=80)
    tap("시작하기", "다음")
    tap("다음", "이 양식으로 시작")
    tap("업무·현장", "다른 양식 보기")
    tap("건너뛰기")
    wait_for("촬영 시작", timeout=80)

def shutter_position():
    root = hierarchy()
    try:
        return target(root, "●")
    except AssertionError:
        # Some emulator accessibility dumps omit the non-clickable inner text.
        # Require the real camera tool icon and derive the centered shutter position
        # in the same bottom control row; never tap a guessed full-screen point.
        x_tool, y_tool = target(root, "촬영 도구")
        size = adb("shell", "wm", "size")
        screen = re.search(r"(\d+)x(\d+)", size)
        if not screen:
            raise AssertionError("Cannot identify screen size for shutter fallback")
        width = int(screen.group(1))
        return width // 2, y_tool

def start_camera():
    tap("촬영 시작")
    wait_for("촬영 도구", timeout=70)
    time.sleep(7)

def root_gallery():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작", timeout=65)
    tap("앨범 보기", "갤러리")
    wait_for("기본 저장", timeout=75)

def long_press_result_tile():
    root = hierarchy()
    titles = [node for node in nodes(root)
              if (node.get("text") or "").startswith("기본 저장 사진")]
    if len(titles) != 1:
        raise AssertionError("Expected exactly one basic storage photo header")
    title_rect = bounds(titles[0])
    if not title_rect:
        raise AssertionError("Cannot locate default photo header")
    y0 = title_rect[3]
    candidates = []
    for item in nodes(root):
        rc = bounds(item)
        if not rc or item.get("clickable") != "true": continue
        x1,y1,x2,y2 = rc
        if y1 >= y0 and 120 <= x2-x1 <= 460 and 120 <= y2-y1 <= 460 and y1 <= y0 + 400:
            candidates.append((y1, x1, rc))
    if not candidates:
        raise AssertionError("No result thumbnail found under basic storage header")
    _,_,(x1,y1,x2,y2) = sorted(candidates)[0]
    x,y=(x1+x2)//2,(y1+y2)//2
    adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "1100")
    wait_for("1장 선택", timeout=35)

def confirm_android_delete():
    limit = time.monotonic()+50
    while time.monotonic() < limit:
        ui = hierarchy()
        for label in ("Delete", "삭제", "Allow", "허용"):
            if any(n.get("text") == label or n.get("content-desc") == label
                   for n in nodes(ui)):
                tap(label)
                return
        time.sleep(1)
    raise AssertionError("Android MediaStore deletion consent not shown")

def check_original_delete():
    check_empty()
    start_camera()
    x, y = shutter_position()
    adb("shell", "input", "tap", str(x), str(y))
    committed = rows_after(2)
    for item in committed: check_file(item)
    assert len(committed) == 2 and len({r["_display_name"] for r in committed}) == 1
    assert sum(r["relative_path"].rstrip("/").endswith("/original") for r in committed) == 1
    mark("01-pair-before-delete")
    root_gallery()
    tap("기본 저장")
    wait_for("기본 저장 사진 1장")
    mark("02-basic-storage")
    tap("뒤로가기", "갤러리")
    tap("기본 저장")
    wait_for("기본 저장 사진 1장")
    long_press_result_tile()
    mark("03-result-selected")
    tap("삭제", "원본 포함 삭제")
    tap("원본 포함 삭제", "총 삭제 파일 2개")
    mark("04-two-files-confirmed")
    tap("삭제")
    confirm_android_delete()
    deadline = time.monotonic()+80
    while time.monotonic() < deadline:
        if not query_media():
            mark("05-pair-deleted")
            print("PASS A: basic storage roundtrip, 1 result + 1 original deleted with consent", flush=True)
            return
        time.sleep(3)
    raise AssertionError("Original-inclusive deletion left MediaStore files behind")

def check_interrupted_capture():
    check_empty()
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작")
    start_camera()
    x,y=shutter_position()
    # Dispatch a real shutter tap, then terminate the sandbox process
    # immediately, without wiping package data or touching the media files.
    adb("shell", f"input tap {x} {y}; sleep 0.15; am force-stop {PKG}", timeout=40)
    time.sleep(2)
    before = query_media()
    mark("06-interrupted")
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    wait_for("촬영 시작", timeout=90)
    time.sleep(7)
    after = query_media()
    if len(after) != len(before):
        raise AssertionError("Recovery unexpectedly created/deleted MediaStore rows without user action")
    if any(row.get("is_pending") != "0" for row in after):
        raise AssertionError("Pending MediaStore rows remain; manual review required")
    if len(after) not in (0,2):
        raise AssertionError(f"Unpaired capture files after interruption: {after}")
    for row in after: check_file(row)
    start_camera()
    tap("●")
    expected = len(after)+2
    deadline=time.monotonic()+95
    result=[]
    while time.monotonic() < deadline:
        result=query_media()
        if len(result)==expected and all(r.get("is_pending")=="0" for r in result): break
        if len(result)>expected: raise AssertionError("Unexpected duplicate files after restart")
        time.sleep(3)
    else:
        raise AssertionError(f"Post-restart capture did not commit a pair: {result}")
    by_name={}
    for row in result:
        check_file(row)
        by_name.setdefault(row["_display_name"], []).append(row)
    if len(by_name) != len(result)//2 or any(len(v)!=2 or
      sum(x["relative_path"].rstrip("/").endswith("/original") for x in v)!=1
      for v in by_name.values()):
        raise AssertionError("Result/original pairing or filename uniqueness failed after restart")
    mark("07-resumed-and-captured")
    print("PASS B: interrupted sandbox process recovered; "
          f"{len(after)} preexisting rows and {len(result)} valid nonduplicate paired rows", flush=True)
    if len(before) == 0:
        print("LIMIT B: capture commit before the kill was not observed; "
              "interruption window may have been pre-commit, not confirmed in-flight I/O.", flush=True)
    else:
        print("B evidence: media had completed a pair before restart; "
              "not proof of a killed in-flight write.", flush=True)

def main():
    # The CI runner is a newly booted emulator. Do not remove any preexisting files.
    check_empty()
    install_once_ready()
    link_exact_folder()
    finish_setup()
    if os.environ.get("ISSUE39_ONLY_INTERRUPT") == "1":
        # Part A passed on run 39080203881. Run B in a clean, independent emulator.
        check_interrupted_capture()
    else:
        check_original_delete()
        check_interrupted_capture()
    print("ISSUE39 MINIMAL QA COMPLETE (isolated emulator only)", flush=True)

if __name__=="__main__":
    try:
        main()
    except Exception:
        save_diagnostics("FAILED-issue39")
        raise
