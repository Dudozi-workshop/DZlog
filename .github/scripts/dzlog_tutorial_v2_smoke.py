#!/usr/bin/env python3
"""Six passive tutorial areas on a fresh disposable Android 16 CI emulator."""
import hashlib
import re
import subprocess
import time

from android16_journey_smoke import PKG, adb, alive, contains, hierarchy, nodes, tap, wait_for
from dzlog_postrelease_full_journey import install_once_ready, link_exact_folder, mark
from android16_capture_media_smoke import query_media


def tap_visible(label):
    """Scroll only the current menu until the exact existing action is visible."""
    for _ in range(6):
        root = hierarchy()
        matches = [n for n in nodes(root) if n.get("text") == label or n.get("content-desc") == label]
        if matches:
            tap(label)
            return
        adb("shell", "input", "swipe", "540", "1750", "540", "600", "300")
    raise AssertionError("Missing guide action: " + label)


def user_preferences():
    result = subprocess.run(["adb", "exec-out", "run-as", PKG, "cat",
                             "files/datastore/dzlog_prefs.preferences_pb"],
                            capture_output=True, check=True, timeout=35)
    return hashlib.sha256(result.stdout).hexdigest()


def open_help():
    tap("설정", "사진 품질")
    tap_visible("사용법 및 도움말")
    wait_for("알아보고 싶은 기능")


def open_session(area, session, title):
    tap_visible(area)
    wait_for("원하는 설명")
    tap_visible(session)
    wait_for(title)
    wait_for("목차")


def back_to_index():
    tap("닫기", "원하는 설명")
    adb("shell", "input", "keyevent", "4")
    wait_for("알아보고 싶은 기능")


def touch_passive_screen():
    root = hierarchy()
    background = next(n for n in nodes(root) if n.get("content-desc") == "기능 안내 화면")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", background.get("bounds")))
    # The floating guide keeps a 16dp margin: these screen-edge taps stay outside it.
    for x, y in ((x1 + 8, (y1 + y2) // 2), (x2 - 8, y2 - 8)):
        adb("shell", "input", "tap", str(x), str(y))
    adb("shell", "input", "keyevent", "24")
    time.sleep(3)  # Let the Android volume overlay dismiss before screenshots.


def main():
    install_once_ready()
    link_exact_folder()
    tap("시작하기", "다음")
    tap("다음", "이 양식으로 시작")
    tap("건너뛰기", "촬영 시작")
    time.sleep(2)
    open_help()
    initial_prefs = user_preferences()
    initial_media = query_media()

    open_session("카메라", "표와 빠른 수정", "표 선택과 배치")
    for title, evidence in (
        ("표 이동", "tutorial-08-table-move"),
        ("표 크기", "tutorial-09-table-size"),
        ("표 회전", "tutorial-10-table-rotation"),
        ("배치 잠금과 해제", "tutorial-11-table-lock"),
        ("상세 편집", "tutorial-12-table-details"),
        ("빠른 수정", "tutorial-13-quick-window"),
        ("문자 입력과 반영", "tutorial-14-quick-text"),
        ("숫자 입력", "tutorial-15-quick-number"),
        ("날짜 형식", "tutorial-16-quick-date"),
        ("촬영 시각", "tutorial-17-quick-time"),
        ("순환문구 세트", "tutorial-18-quick-phrase"),
        ("파일명 전체 확인", "tutorial-19-quick-name"),
        ("저장경로 전체 확인", "tutorial-20-quick-path"),
        ("템플릿 변경", "tutorial-21-quick-template"),
    ):
        tap("다음", title)
        if title == "빠른 수정":
            touch_passive_screen()
        time.sleep(4)  # Allow the one-shot presentation to finish.
        mark(evidence)
    back_to_index()

    open_session("홈", "촬영 준비", "현재 템플릿")
    touch_passive_screen()
    mark("tutorial-01-home")
    for title in ("템플릿 선택", "템플릿 관리", "수정과 삭제", "다음 촬영번호", "촬영 시작"):
        tap("다음", title)
    tap("완료", "최근 촬영")
    # Previous crosses the group boundary, restoring its last explanation.
    tap("이전", "촬영 시작")
    tap("완료", "최근 촬영")
    tap("닫기", "원하는 설명")
    if not contains(hierarchy(), "완료"):
        raise AssertionError("Finished session status was not retained")
    adb("shell", "input", "keyevent", "4")
    wait_for("알아보고 싶은 기능")

    open_session("카메라", "초점과 촬영도구", "배율 칩")
    touch_passive_screen()
    time.sleep(4)
    mark("tutorial-02a-camera-zoom")
    for title in ("핀치 줌", "초점 조절", "도구 메뉴", "초점 도구", "플래시", "격자"):
        tap("다음", title)
    mark("tutorial-02-camera-tools")
    back_to_index()

    for area, session, title, evidence in (
        ("앨범", "앨범 화면과 탐색", "갤러리 첫 화면", "tutorial-03-gallery"),
        ("표 편집", "행·열과 병합 구조", "표 구조", "tutorial-04-table"),
        ("저장설정", "파일명 구성", "저장설정 개요", "tutorial-05-saving"),
        ("앱 설정", "데이터 관리와 도움말", "작업 환경 백업", "tutorial-06-settings"),
    ):
        open_session(area, session, title)
        touch_passive_screen()
        mark(evidence)
        back_to_index()

    if user_preferences() != initial_prefs:
        raise AssertionError("Tutorial altered camera settings, counters, phrases or templates")
    if query_media() != initial_media:
        raise AssertionError("Tutorial created, changed or deleted photos")
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", f"{PKG}/.MainActivity", timeout=70)
    wait_for("촬영 시작")
    open_help()
    open_session("카메라", "초점과 촬영도구", "격자")
    mark("tutorial-07-progress-after-relaunch")
    alive()
    print("PASS six areas; previous/next/completion; background and volume input blocked; preferences/media unchanged; progress survives relaunch", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            crash = adb("logcat", "-d", "-b", "crash", "-v", "threadtime")
            print("ANDROID CRASH LOG\n" + crash, flush=True)
            from pathlib import Path
            Path("postrelease-full-e2e/tutorial-crash-log.txt").write_text(crash)
            mark("tutorial-FAILED")
        except Exception:
            pass
        raise
