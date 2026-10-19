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
    wait_for("궁금한 기능")


def open_session(area, session, title):
    tap_visible(area)
    wait_for("안내 진행도")
    if area == "홈":
        mark("tutorial-44-home-index")
    tap_visible(session)
    wait_for(title)
    wait_for("목차")


def back_to_index():
    tap("닫기", "안내 진행도")
    adb("shell", "input", "keyevent", "4")
    wait_for("궁금한 기능")


def touch_passive_screen():
    root = hierarchy()
    background = next(n for n in nodes(root) if n.get("content-desc") == "기능 안내 화면")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", background.get("bounds")))
    # The floating guide keeps a 16dp margin: these screen-edge taps stay outside it.
    for x, y in ((x1 + 8, (y1 + y2) // 2), (x2 - 8, y2 - 8)):
        adb("shell", "input", "tap", str(x), str(y))
    adb("shell", "input", "keyevent", "24")
    time.sleep(3)  # Let the Android volume overlay dismiss before screenshots.


def guide_button_y():
    node = next(n for n in nodes(hierarchy()) if n.get("text") == "다음")
    return tuple(map(int, re.findall(r"\d+", node.get("bounds"))))[1::2]


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

    # Camera follows actual screen direction: top controls, preview, then controls.
    # Each button advances just one guide topic; passive demonstrations cannot save.
    open_session("카메라", "상단 정보·촬영 설정", "다음 촬영 정보")
    bottom_buttons = guide_button_y()
    mark("tutorial-30-next-info")
    for title, evidence in (("촬영 비율", "tutorial-25-aspect"),
                            ("저장 방식", "tutorial-26-save-format"),
                            ("촬영 후 확인 방식", "tutorial-27-preview-mode"),
                            ("촬영 효과음", "tutorial-28-sound"),
                            ("촬영 진동", "tutorial-28-haptic"),
                            ("음량 버튼", "tutorial-29-volume")):
        tap("다음", title)
        time.sleep(3)
        mark(evidence)
        if any(abs(a - b) > 4 for a, b in zip(guide_button_y(), bottom_buttons)):
            raise AssertionError("Top settings guide controls shifted unexpectedly")
    tap("다음", "촬영도구 메뉴")
    tap("이전", "음량 버튼")
    back_to_index()

    open_session("카메라", "촬영 화면·도구", "촬영도구 메뉴")
    top_buttons = guide_button_y()
    for title, evidence in (("초점", "tutorial-02b-camera-focus"),
                            ("플래시", "tutorial-02c-camera-flash"),
                            ("격자", "tutorial-02d-camera-grid"),
                            ("표 표시", "tutorial-02e-camera-table"),
                            ("보조 셔터", "tutorial-02f-camera-assist"),
                            ("배율 조절", "tutorial-02a-camera-zoom")):
        tap("다음", title)
        time.sleep(3)
        mark(evidence)
        if any(abs(a - b) > 4 for a, b in zip(guide_button_y(), top_buttons)):
            raise AssertionError("Camera tool guide controls shifted unexpectedly")
    back_to_index()

    open_session("카메라", "하단 버튼·촬영 확인", "카메라 화면")
    for title, evidence in (("촬영과 저장", "tutorial-22-saving-status"),
                            ("촬영 후 확인", "tutorial-23-preview-example"),
                            ("최근 촬영", "tutorial-23-recent"),
                            ("촬영 취소", "tutorial-24-undo-warning")):
        tap("다음", title)
        time.sleep(3)
        mark(evidence)
    back_to_index()

    open_session("카메라", "표와 빠른 수정", "표 선택")
    for title, evidence in (
        ("이동과 크기", "tutorial-09-table-size"),
        ("회전과 잠금", "tutorial-11-table-lock"),
        ("상세 편집", "tutorial-12-table-details"),
        ("빠른 수정 열기", "tutorial-13-quick-window"),
        ("템플릿 변경", "tutorial-21-quick-template"),
        ("파일명 확인", "tutorial-21-quick-filename"),
        ("저장경로 확인", "tutorial-21-quick-path"),
        ("텍스트 입력", "tutorial-21-quick-text"),
        ("숫자 입력", "tutorial-21-quick-number"),
        ("날짜와 시간", "tutorial-21-quick-date"),
        ("순환문구", "tutorial-21-quick-phrase"),
    ):
        tap("다음", title)
        time.sleep(3)
        mark(evidence)
    touch_passive_screen()
    back_to_index()

    open_session("홈", "촬영 준비", "현재 템플릿")
    touch_passive_screen()
    mark("tutorial-01-home")
    bottom_buttons = guide_button_y()
    for title in ("템플릿 선택", "템플릿 메뉴", "템플릿 관리", "다음 촬영번호", "촬영 시작"):
        tap("다음", title)
        if title != "촬영 시작" and any(abs(a - b) > 4 for a, b in zip(bottom_buttons, guide_button_y())):
            raise AssertionError("Home bottom guide buttons moved between explanations")
        if title in ("템플릿 선택", "템플릿 메뉴", "템플릿 관리"):
            mark({"템플릿 선택": "tutorial-37-template-list",
                  "템플릿 메뉴": "tutorial-38-template-button",
                  "템플릿 관리": "tutorial-39-template-menu"}[title])
    tap("다음", "최근 촬영")
    # Previous crosses the group boundary, restoring its last explanation.
    tap("이전", "촬영 시작")
    tap("다음", "최근 촬영")
    tap("닫기", "안내 진행도")
    tap("이어서 보기", "최근 촬영")
    mark("tutorial-40-home-recent")
    top_buttons = guide_button_y()
    tap("다음", "앨범 보기")
    mark("tutorial-41-home-album")
    if any(abs(a - b) > 4 for a, b in zip(top_buttons, guide_button_y())):
        raise AssertionError("Recent photo and album guide positions differ")
    tap("다음", "표 편집")
    tap("다음", "저장 정보")
    mark("tutorial-42-home-save-info")
    tap("다음", "앱 설정")
    tap("완료", "홈 목차")
    mark("tutorial-43-home-contents")
    if contains(hierarchy(), "카메라"):
        raise AssertionError("Home contents unexpectedly expands other areas")
    tap("현재 템플릿", "현재 템플릿")
    wait_for("목차")
    tap("닫기", "안내 진행도")
    if not contains(hierarchy(), "완료"):
        raise AssertionError("Finished session status was not retained")
    adb("shell", "input", "keyevent", "4")
    wait_for("궁금한 기능")

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

    # New shared dialogs must stay behind the passive input shield and guide buttons.
    open_session("앨범", "폴더와 하위 폴더 관리", "폴더 관리")
    tap("다음", "새 폴더")
    touch_passive_screen()
    mark("tutorial-30-new-folder-dialog")
    back_to_index()
    open_session("표 편집", "저장·취소와 실행 취소", "저장")
    for title in ("편집 실행 취소", "편집 다시 실행", "저장하고 나가기"):
        tap("다음", title)
    touch_passive_screen()
    mark("tutorial-31-unsaved-decision-dialog")
    back_to_index()

    open_session("저장설정", "파일명 구성", "저장설정 개요")
    for title in ("파일명 구성", "구성요소", "출처 추가", "값과 유형", "순서 변경", "요소 삭제", "결과 미리보기", "저장경로 구성 가져오기"):
        tap("다음", title)
    touch_passive_screen()
    mark("tutorial-32-save-rule-copy")
    back_to_index()
    open_session("저장설정", "자동번호와 동기화", "자동번호")
    for title in ("다음 번호", "번호 직접 입력"):
        tap("다음", title)
    mark("tutorial-33-counter-input")
    for title in ("자릿수", "번호 동기화", "번호 초기화"):
        tap("다음", title)
    touch_passive_screen()
    mark("tutorial-34-counter-reset")
    back_to_index()
    open_session("앱 설정", "데이터 관리와 도움말", "작업 환경 백업")
    tap("다음", "작업 환경 내보내기")
    touch_passive_screen()
    mark("tutorial-35-backup-export")
    for title in ("작업 환경 가져오기", "작업 환경 가져오기 화면"):
        tap("다음", title)
    mark("tutorial-36-backup-import")
    back_to_index()

    if user_preferences() != initial_prefs:
        raise AssertionError("Tutorial altered camera settings, counters, phrases or templates")
    if query_media() != initial_media:
        raise AssertionError("Tutorial created, changed or deleted photos")
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", f"{PKG}/.MainActivity", timeout=70)
    wait_for("촬영 시작")
    open_help()
    open_session("카메라", "촬영 화면·도구", "화면 터치 초점")
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
