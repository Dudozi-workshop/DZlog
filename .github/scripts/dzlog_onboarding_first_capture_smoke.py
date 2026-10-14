#!/usr/bin/env python3
"""ON-04 optional camera coaching and actual first saved-result regression."""
import time

from android16_journey_smoke import adb, contains, hierarchy, nodes, tap, wait_for
from android16_capture_media_smoke import check_both, query_media, rows_after
from dzlog_postrelease_full_journey import install_once_ready, link_exact_folder, mark


def main():
    install_once_ready()
    link_exact_folder()
    tap("시작하기", "다음")
    tap("다음", "이 양식으로 시작")
    tap("이 양식으로 시작")
    first = wait_for("첫 촬영 · 1/2", timeout=100)
    if not contains(first, "빠른 수정 없이 촬영하기"):
        raise AssertionError("First-shutter action must remain optional")
    mark("ON04-01-quick-edit-optional")
    if query_media():
        raise AssertionError("ON-04 must not shoot/save merely by opening")
    tap("빠른 수정 없이 촬영하기", "첫 촬영 · 2/2")
    mark("ON04-02-shutter-optional")
    time.sleep(7)
    tap("●")
    rows = rows_after(2, max_seconds=140)
    check_both(rows, 1)
    root = wait_for("첫 사진 저장 완료", timeout=75)
    mark("ON04-03-saved-receipt")
    texts = [node.get("text") or node.get("content-desc") or "" for node in nodes(root)]
    reported = [value.partition("파일명: ")[2] for value in texts
                if value.startswith("파일명: ")]
    paths = [value.partition("저장 위치: ")[2] for value in texts
             if value.startswith("저장 위치: ")]
    if len(reported) != 1 or reported[0] not in {
        item.get("_display_name", "") for item in rows
    }:
        raise AssertionError("Saved guide did not report actual persisted filename")
    if len(paths) != 1 or paths[0] not in {
        item.get("relative_path", "") for item in rows
    }:
        raise AssertionError("Saved guide did not report actual persisted folder")
    tap("상세 사용법 알아보기", "사용법 및 도움말")
    wait_for("카메라 촬영", timeout=40)
    mark("ON04-04-help-index")
    tap("카메라 촬영", "단계 1/4")
    mark("ON04-05-help-module")
    print("PASS ON-04 optional first camera, actual commit filename/path, help module", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            mark("ON04-FAILED")
        except Exception:
            pass
        raise
