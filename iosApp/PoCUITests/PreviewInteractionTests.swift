import XCTest
import Vision

// Full-suite checkpoint after extending quick edit parity and template switching.
final class PreviewInteractionTests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        // One durable metadata namespace per test, retained through terminate/launch.
        // Only the explicit configured-capture fixture consumes this environment key.
        app.launchEnvironment["DZLOG_UI_TEST_MEDIA_NAMESPACE"] = UUID().uuidString
        // PhotoKit may present its asynchronous prompt after the explicit wait.
        // Always handle it before XCTest's default handler selects Don't Allow.
        addUIInterruptionMonitor(withDescription: "Photo library access") { alert in
            for name in ["Allow Full Access", "Allow Access to All Photos", "Allow Access to All Photos and Videos"] {
                if alert.buttons[name].exists { alert.buttons[name].tap(); return true }
            }
            return false
        }
    }

    override func tearDown() {
        guard app.state != .notRunning else {
            let note = XCTAttachment(string: "Application was not running at teardown; preserve the original test failure.")
            note.lifetime = .keepAlways
            add(note)
            return
        }
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = name
        image.lifetime = .keepAlways
        add(image)
        let hierarchyDescription = app.debugDescription
        let hierarchy = XCTAttachment(string: hierarchyDescription)
        print("UI_HIERARCHY: \(hierarchyDescription)")
        hierarchy.lifetime = .keepAlways
        add(hierarchy)
        app.terminate()
    }

    // Only the sensor JPEG is synthetic. Exercise the user settings form and
    // production configured save entry, then relaunch and read the real gallery.
    // These quick-value checks are the focused gate; milestone validation still
    // runs this class as part of the complete, unfiltered UI suite.
    func testQuickValuesCancelApplySaveAndRelaunch() {
        // Exercise the real authorization prompt even when run after the denial test.
        app.resetAuthorizationStatus(for: .photos)
        app.launchArguments = ["--quick-values-flow", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("빠른 입력 열기")
        tapPanelText("2행 1열 셀 선택")
        tapPanelText("2행 1열 · 텍스트")
        typeWithVisibleKeyboard("blue")
        tapButton("취소")
        XCTAssertTrue(text("상태: 대기").waitForExistence(timeout: 20))
        XCTAssertFalse(text("입력값: blue").exists)
        tapButton("빠른 입력 열기")
        tapPanelText("2행 1열 셀 선택")
        tapPanelText("2행 1열 · 텍스트")
        typeWithVisibleKeyboard("blue")
        tapButton("완료")
        XCTAssertTrue(text("입력값: blue").waitForExistence(timeout: 20))
        XCTAssertTrue(text("숫자값: 1.20").exists)
        XCTAssertTrue(text("다음 번호: 12").exists)
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(
            format: "label BEGINSWITH %@ AND label CONTAINS %@", "파일명: ", "blue")).firstMatch.exists)
        tapButton("빠른 입력 열기")
        tapPanelText("1행 3열 셀 선택")
        tapScrollableButton("날짜 형식 · yyyyMMdd")
        tapScrollableButton("yyMMdd · 짧은 연월일")
        tapButton("다음 셀")
        tapScrollableButton("문구 세트 · 과수원")
        tapScrollableButton("온실")
        tapButton("완료")
        XCTAssertTrue(text("날짜형식: yyMMdd").waitForExistence(timeout: 20))
        XCTAssertTrue(text("문구세트: set-b").exists)
        tapButton("입력값으로 촬영 저장")
        XCTAssertTrue(waitForPhotoState("상태: 입력 반영 사진 저장 완료"), app.debugDescription)
        XCTAssertTrue(text("다음 번호: 13").exists)
        app.terminate()
        app.launchArguments = ["--quick-values-flow"]
        app.launch()
        XCTAssertTrue(text("입력값: blue").waitForExistence(timeout: 20))
        XCTAssertTrue(text("숫자값: 1.20").exists)
        XCTAssertTrue(text("날짜형식: yyMMdd").exists)
        XCTAssertTrue(text("문구세트: set-b").exists)
        XCTAssertTrue(text("다음 번호: 13").exists)
    }

    func testQuickInputKeepsKeyboardAcrossManualCellsAndCancelsPhraseSelection() {
        app.launchArguments = ["--quick-values-flow", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("빠른 입력 열기")
        tapPanelText("2행 1열 셀 선택")
        tapScrollableButton("입력창 확대")
        XCTAssertTrue(button("입력창 축소").exists)
        tapScrollableButton("입력창 축소")
        tapPanelText("2행 1열 · 텍스트")
        typeWithVisibleKeyboard("blue")
        tapButton("다음 셀")
        XCTAssertTrue(panelText("2행 2열 · 숫자").waitForExistence(timeout: 10))
        typeWithVisibleKeyboard("3")
        tapButton("이전 셀")
        typeWithVisibleKeyboard("x")
        let keyboard = app.keyboards.firstMatch
        XCTAssertTrue(keyboard.exists, "Manual cell switching must keep the keyboard open")
        // The secondary Input action is below the keyboard viewport. Select
        // the visible automatic cell instead and verify the keyboard closes.
        tapPanelText("1행 4열 셀 선택")
        let closed = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: keyboard)
        XCTAssertEqual(XCTWaiter.wait(for: [closed], timeout: 10), .completed, app.debugDescription)
        let preview = app.descendants(matching: .any).matching(NSPredicate(
            format: "label BEGINSWITH %@ AND label CONTAINS %@", "파일명 · ", "bluex")).firstMatch
        XCTAssertTrue(preview.waitForExistence(timeout: 10))
        tapScrollableButton("문구 세트 · 과수원")
        tapScrollableButton("온실")
        tapButton("취소")
        XCTAssertTrue(text("문구세트: set-a").waitForExistence(timeout: 10))
        XCTAssertTrue(text("숫자값: 1.20").exists)
        XCTAssertFalse(text("입력값: bluex").exists)
    }

    func testPhraseCsvFilesPickersCanCancelWithoutReplacingTheSet() {
        app.launchArguments = ["--quick-values-flow", "--phrase-csv-picker"]
        app.launch()
        tapPanelText("CSV 일괄 관리")
        for label in ["CSV 불러오기", "CSV 내보내기"] {
            tapButton(label)
            // Files exposes the remote toolbar Cancel separately from a proxy over More.
            let navCancel = app.navigationBars.buttons.matching(NSPredicate(format: "label IN %@", ["Cancel", "취소"])).firstMatch
            let grabber = app.buttons.matching(NSPredicate(format: "label IN %@", ["Sheet Grabber", "시트 이동 막대"])).firstMatch
            let control = label == "CSV 불러오기" ? navCancel : grabber
            guard control.waitForExistence(timeout: 15) else {
                XCTFail("Missing Files control for \(label). \(app.debugDescription)")
                return
            }
            if label == "CSV 불러오기" {
                navCancel.tap()
            } else {
                grabber.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
                    .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.9)))
            }
            let returnedToCsv = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
                !control.exists && self.app.buttons["CSV 내보내기"].isEnabled && self.app.buttons["CSV 불러오기"].isEnabled
            }, object: nil)
            XCTAssertEqual(XCTWaiter.wait(for: [returnedToCsv], timeout: 10), .completed, app.debugDescription)
        }
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.18)).tap()
        tapButton("뒤로가기")
        XCTAssertTrue(text("과수원 · 온실").waitForExistence(timeout: 10))
    }

    func testQuickGridWrapAndDraftAcrossCells() {
        app.launchArguments = ["--quick-values-flow", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("빠른 입력 열기")
        tapButton("이전 셀")
        XCTAssertTrue(text("촬영 시각으로 자동 입력돼요.").waitForExistence(timeout: 10))
        tapButton("다음 셀")
        XCTAssertTrue(panelText("1행 1~2열 · 텍스트").waitForExistence(timeout: 10))
        XCTAssertFalse(panelText("1행 2열 셀 선택").exists)
        tapPanelText("2행 2열 셀 선택")
        tapButton("±")
        tapButton("다음 셀")
        XCTAssertTrue(text("촬영을 저장하면 자동으로 증가해요.").waitForExistence(timeout: 10))
        tapButton("완료")
        XCTAssertTrue(text("숫자값: -1.20").waitForExistence(timeout: 10))
        XCTAssertTrue(text("다음 번호: 12").exists)
        tapButton("빠른 입력 열기")
        XCTAssertTrue(text("촬영을 저장하면 자동으로 증가해요.").waitForExistence(timeout: 10))
    }

    func testQuickAutomaticDraftWrapCancelAndRelaunch() {
        app.launchArguments = ["--quick-values-flow", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("빠른 입력 열기")
        XCTAssertTrue(text("저장경로 · Pictures/DZlog/\nPictures/DZlog/original/").waitForExistence(timeout: 10))
        tapPanelText("1행 3열 셀 선택")
        tapScrollableButton("날짜 형식 · yyyyMMdd")
        tapScrollableButton("yyMMdd · 짧은 연월일")
        tapButton("다음 셀")
        tapScrollableButton("문구 세트 · 과수원")
        tapScrollableButton("온실")
        tapPanelText("2행 4열 셀 선택")
        XCTAssertTrue(text("촬영 시각으로 자동 입력돼요.").waitForExistence(timeout: 10))
        tapButton("다음 셀")
        XCTAssertTrue(panelText("1행 1~2열 · 텍스트").waitForExistence(timeout: 10))
        tapButton("다음 셀")
        XCTAssertTrue(text("날짜 형식 · yyMMdd").waitForExistence(timeout: 10))
        tapButton("다음 셀")
        XCTAssertTrue(text("문구 세트 · 온실").waitForExistence(timeout: 10))
        tapButton("취소")
        XCTAssertTrue(text("날짜형식: yyyyMMdd").waitForExistence(timeout: 10))
        XCTAssertTrue(text("문구세트: set-a").exists)

        app.terminate()
        app.launchArguments = ["--quick-values-flow"]
        app.launch()
        XCTAssertTrue(text("날짜형식: yyyyMMdd").waitForExistence(timeout: 10))
        XCTAssertTrue(text("문구세트: set-a").exists)
        XCTAssertTrue(text("다음 번호: 12").exists)
    }

    func testQuickTextFavoriteSurvivesCancellationAndRelaunch() {
        app.launchArguments = ["--quick-values-flow", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("빠른 입력 열기")
        tapPanelText("2행 1열 셀 선택")
        tapPanelText("2행 1열 · 텍스트")
        typeWithVisibleKeyboard("blue")
        tapButton("즐겨찾기 즉시 등록")
        tapButton("취소")
        XCTAssertFalse(text("입력값: blue").exists)

        app.terminate()
        app.launchArguments = ["--quick-values-flow"]
        app.launch()
        tapButton("빠른 입력 열기")
        tapPanelText("2행 1열 셀 선택")
        tapPanelText("blue")
        tapButton("완료")
        XCTAssertTrue(text("입력값: blue").waitForExistence(timeout: 20))
    }

    func testQuickValuesEligibilityNumbersAndStaleWorkspace() {
        app.launchArguments = ["--quick-values-flow", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("입력 정책 검증")
        XCTAssertTrue(text("상태: 편집 대상·숫자·충돌 차단 검증 완료").waitForExistence(timeout: 20), app.debugDescription)
        XCTAssertTrue(text("숫자값: 1.20").exists)
        XCTAssertTrue(text("다음 번호: 12").exists)
    }

    func testQuickValueTemplateSwitchPersistsDraftAndSelection() {
        app.launchArguments = ["--quick-values-flow", "--quick-project-switch", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("빠른 입력 열기")
        XCTAssertTrue(text("저장경로 · Pictures/DZlog/orchard/").waitForExistence(timeout: 10))
        tapPanelText("2행 1열 셀 선택")
        tapPanelText("2행 1열 · 텍스트")
        typeWithVisibleKeyboard("blue")
        tapButton("템플릿 · 빠른 입력 검증")
        tapButton("빠른 입력 보조")
        XCTAssertTrue(text("저장경로 · Pictures/DZlog/lab/original/").waitForExistence(timeout: 10))
        tapButton("완료")
        XCTAssertTrue(text("활성템플릿: 빠른 입력 보조").waitForExistence(timeout: 20))
        XCTAssertTrue(text("입력값: SECOND").exists)
        XCTAssertTrue(text("다음 번호: 40").exists)
        app.terminate()
        app.launchArguments = ["--quick-values-flow", "--quick-project-switch"]
        app.launch()
        XCTAssertTrue(text("활성템플릿: 빠른 입력 보조").waitForExistence(timeout: 20))
        XCTAssertTrue(text("경로: Pictures/DZlog/lab/").exists)
        tapButton("일반 촬영으로")
        XCTAssertTrue(text("경로: Pictures/DZlog/orchard/").waitForExistence(timeout: 10))
        XCTAssertTrue(text("입력값: blue").exists)
        XCTAssertTrue(text("다음 번호: 12").exists)
        tapButton("조사 프로젝트로")
        XCTAssertTrue(text("경로: Pictures/DZlog/lab/").waitForExistence(timeout: 10))
        XCTAssertTrue(text("다음 번호: 40").exists)
        tapButton("빠른 입력 열기")
        tapButton("템플릿 · 빠른 입력 보조")
        tapButton("빠른 입력 검증")
        tapButton("완료")
        XCTAssertTrue(text("활성템플릿: 빠른 입력 검증").waitForExistence(timeout: 20))
        XCTAssertTrue(text("입력값: blue").exists)
        XCTAssertTrue(text("다음 번호: 12").exists)
    }

    private func liveHandle(_ identifier: String) -> XCUIElement {
        // Kotlin's bundled UIKit interface exposes these native accessibility labels.
        // Each handle has a unique label; frame/value/drag assertions remain unchanged.
        let label = identifier == "ios.table.move" ? "표 이동" : "표 크기 조절"
        return app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    private func openLiveTable(reset: Bool, useFocusTimer: Bool = false, simulateFocusOutcomes: Bool = false) {
        app.launchArguments = ["--live-table-placement-flow"] + (reset ? ["--reset-live-table-fixture"] : [])
        if useFocusTimer { app.launchArguments.append("--live-focus-timer") }
        if simulateFocusOutcomes { app.launchArguments.append("--live-focus-outcomes") }
        app.launch()
        XCTAssertTrue(text("표 준비 완료").waitForExistence(timeout: 20), app.debugDescription)
        XCTAssertTrue(liveHandle("ios.table.move").waitForExistence(timeout: 10))
        XCTAssertTrue(text("촬영 표 미리보기").exists)
    }

    private func dragLiveHandle(_ identifier: String, dx: CGFloat, dy: CGFloat) {
        let handle = liveHandle(identifier)
        XCTAssertTrue(handle.waitForExistence(timeout: 10), app.debugDescription)
        let frame = handle.frame
        XCTAssertTrue(app.frame.contains(frame))
        let from = app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: frame.midX, dy: frame.midY))
        let to = app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: frame.midX + dx, dy: frame.midY + dy))
        from.press(forDuration: 0.15, thenDragTo: to)
        XCTAssertTrue(text("상태: 표 배치 저장 완료").waitForExistence(timeout: 20), app.debugDescription)
    }

    private func livePreview() -> XCUIElement {
        let preview = app.descendants(matching: .any).matching(NSPredicate(
            format: "label == %@", "촬영 표 미리보기"
        )).firstMatch
        XCTAssertTrue(preview.waitForExistence(timeout: 10), app.debugDescription)
        return preview
    }

    private func tapLivePreview(_ x: CGFloat, _ y: CGFloat) {
        let preview = livePreview()
        preview.coordinate(withNormalizedOffset: CGVector(dx: x, dy: y)).tap()
    }

    func testNativeLiveTableMoveResizePersistsAndSaves() {
        openLiveTable(reset: true)
        let initialMove = String(describing: liveHandle("ios.table.move").value ?? "")
        dragLiveHandle("ios.table.move", dx: 55, dy: 45)
        let moved = String(describing: liveHandle("ios.table.move").value ?? "")
        XCTAssertNotEqual(moved, initialMove)
        let beforeResize = liveHandle("ios.table.move").frame
        let initialSize = String(describing: liveHandle("ios.table.resize").value ?? "")
        let handle = liveHandle("ios.table.resize").frame
        // Drag the active handle toward the table center to shrink from any corner.
        dragLiveHandle("ios.table.resize", dx: (beforeResize.midX - handle.midX) / 2,
                       dy: (beforeResize.midY - handle.midY) / 2)
        let resized = String(describing: liveHandle("ios.table.resize").value ?? "")
        let resizedPosition = String(describing: liveHandle("ios.table.move").value ?? "")
        let afterResize = liveHandle("ios.table.move").frame
        XCTAssertNotEqual(resized, initialSize)
        XCTAssertLessThan(afterResize.width, beforeResize.width)
        XCTAssertLessThan(afterResize.height, beforeResize.height)
        // Offsets are percentages of the remaining free space. Resizing preserves
        // the displayed center and recomputes those percentages, rounded to 1%.
        let roundingTolerance = max(app.frame.width, app.frame.height) / 200 + 1
        XCTAssertEqual(afterResize.midX, beforeResize.midX, accuracy: roundingTolerance)
        XCTAssertEqual(afterResize.midY, beforeResize.midY, accuracy: roundingTolerance)
        XCTAssertTrue(text("다음 번호: 12").exists)
        tapButton("이 배치로 촬영 저장")
        XCTAssertTrue(waitForPhotoState("상태: 표 배치·사진 저장 완료"), app.debugDescription)
        XCTAssertTrue(text("다음 번호: 13").waitForExistence(timeout: 20))
        app.terminate()
        openLiveTable(reset: false)
        XCTAssertEqual(String(describing: liveHandle("ios.table.move").value ?? ""), resizedPosition)
        XCTAssertEqual(String(describing: liveHandle("ios.table.resize").value ?? ""), resized)
        XCTAssertTrue(text("다음 번호: 13").exists)
    }

    func testNativeLiveTableRotationLockAndGeometryBounds() {
        openLiveTable(reset: true)
        tapButton("배치 계산 검증")
        XCTAssertTrue(text("상태: 배치 계산·비율 차단 검증 완료").waitForExistence(timeout: 20), app.debugDescription)
        tapButton("90도 전환")
        XCTAssertTrue(text("회전: 90").waitForExistence(timeout: 20))
        XCTAssertTrue(text("표 준비 완료").waitForExistence(timeout: 20))
        let box = liveHandle("ios.table.move").frame
        XCTAssertGreaterThan(box.height, box.width)
        let value = String(describing: liveHandle("ios.table.move").value ?? "")
        tapButton("조작 잠금")
        XCTAssertFalse(liveHandle("ios.table.move").isHittable)
        XCTAssertTrue(text("촬영 표 미리보기").exists)
        tapButton("조작 잠금 해제")
        XCTAssertTrue(liveHandle("ios.table.move").waitForExistence(timeout: 10))
        XCTAssertEqual(String(describing: liveHandle("ios.table.move").value ?? ""), value)
        dragLiveHandle("ios.table.move", dx: 30, dy: 30)
        XCTAssertTrue(text("다음 번호: 12").exists)
    }

    func testNativeFocusControlsCollapseAfterInteraction() {
        openLiveTable(reset: true, useFocusTimer: true)
        let frame = livePreview().frame
        XCTAssertFalse(frame.isEmpty)
        let point = CGPoint(x: frame.minX + frame.width * 0.08, y: frame.minY + frame.height * 0.08)
        app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: point.x - app.frame.minX, dy: point.y - app.frame.minY)).tap()
        // XCTest may first sample after the 1.2-second result display has elapsed.
        XCTAssertTrue(text("상태: 터치 초점 적용").waitForExistence(timeout: 5))
        XCTAssertTrue(app.sliders["밝기 조절"].waitForNonExistence(timeout: 5))
        XCTAssertEqual(text("카메라 초점").frame.width, 40, accuracy: 1)
    }

    func testNativeFocusFeedbackFailureTimeoutAndRecovery() {
        openLiveTable(reset: true, simulateFocusOutcomes: true)
        let preview = livePreview().frame
        func focus(_ x: CGFloat) {
            let point = CGVector(dx: preview.minX + preview.width * x - app.frame.minX,
                                 dy: preview.minY + preview.height * 0.76 - app.frame.minY)
            app.coordinate(withNormalizedOffset: .zero).withOffset(point).tap()
        }
        let ring = text("카메라 초점")
        focus(0.10)
        XCTAssertTrue(ring.waitForExistence(timeout: 5))
        XCTAssertEqual(ring.value as? String, "초점 요청 실패")
        XCTAssertTrue(button("노출 잠금").isEnabled)
        focus(0.90)
        XCTAssertTrue(text("상태: 초점 응답 대기").waitForExistence(timeout: 5))
        XCTAssertTrue(ring.waitForNonExistence(timeout: 5))
        XCTAssertFalse(app.sliders["밝기 조절"].exists)
        focus(0.50)
        XCTAssertTrue(ring.waitForExistence(timeout: 5))
        XCTAssertEqual(ring.value as? String, "초점 조정 완료")
        XCTAssertTrue(app.sliders["밝기 조절"].exists)
    }

    func testNativeFocusControlsAtPreviewEdges() {
        openLiveTable(reset: true)
        let preview = livePreview().frame
        let slider = app.sliders["밝기 조절"]
        for (x, y) in [(0.5, 0.5), (0.01, 0.01), (0.99, 0.99)] as [(CGFloat, CGFloat)] {
            let point = CGVector(dx: preview.minX + x * preview.width - app.frame.minX,
                                 dy: preview.minY + y * preview.height - app.frame.minY)
            app.coordinate(withNormalizedOffset: .zero).withOffset(point).tap()
            XCTAssertTrue(slider.waitForExistence(timeout: 5))
            let ring = text("카메라 초점").frame
            let lock = button("노출 잠금").frame
            XCTAssertEqual(ring.width, 44, accuracy: 1)
            XCTAssertEqual(lock.width, 48, accuracy: 1)
            XCTAssertEqual(slider.frame.width, 108, accuracy: 1)
            for rect in [ring, lock, slider.frame] {
                XCTAssertTrue(preview.insetBy(dx: -1, dy: -1).contains(rect))
            }
            XCTAssertEqual(ring.midX, min(max(preview.minX + x * preview.width, preview.minX + 22),
                                          preview.maxX - 22), accuracy: 1)
            XCTAssertEqual(lock.midY, y < 0.02 ? ring.maxY : ring.minY, accuracy: 1)
            if y > 0.98 {
                XCTAssertLessThan(slider.frame.maxY, lock.minY)
            } else {
                XCTAssertGreaterThan(slider.frame.minY, lock.maxY)
            }
        }
        tapButton("노출 잠금")
        tapButton("초점 최소화")
        tapButton("밝기 조절 열기")
        XCTAssertTrue(slider.waitForExistence(timeout: 5))
        XCTAssertTrue(button("노출 잠금 해제").exists)
    }

    func testNativeFocusExposureLockUsesConfirmedTwoTapPolicy() {
        openLiveTable(reset: true)
        func tapFocusPoint(_ x: CGFloat, _ y: CGFloat) {
            // The overlay UIImageView is not an interactive UIKit view. Use its
            // observed screen frame, not XCTest's element-relative hit-point routing.
            let frame = livePreview().frame
            XCTAssertFalse(frame.isEmpty)
            XCTAssertTrue(app.frame.contains(frame))
            let point = CGPoint(x: frame.minX + frame.width * x, y: frame.minY + frame.height * y)
            for control in [text("표 이동"), text("표 크기 조절"),
                            app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "노출 잠금")).firstMatch,
                            app.sliders["밝기 조절"]] {
                if control.exists { XCTAssertFalse(control.frame.contains(point), "Focus point overlaps a native control") }
            }
            app.coordinate(withNormalizedOffset: .zero)
                .withOffset(CGVector(dx: point.x - app.frame.minX, dy: point.y - app.frame.minY)).tap()
        }
        tapFocusPoint(0.08, 0.08)
        XCTAssertTrue(text("상태: 터치 초점 적용").waitForExistence(timeout: 10), app.debugDescription)
        XCTAssertTrue(text("카메라 초점").exists)
        XCTAssertTrue(button("노출 잠금").exists)
        XCTAssertTrue(app.sliders["밝기 조절"].exists)

        tapButton("노출 잠금")
        XCTAssertTrue(text("상태: 노출 잠금").waitForExistence(timeout: 10))
        let brightness = app.sliders["밝기 조절"]
        brightness.adjust(toNormalizedSliderPosition: 0.75)
        // Compose text is exposed as Other on some native accessibility bridges.
        // Keep checking the observed bias, independent of its element type.
        let biasLabel = app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH %@", "밝기 보정:")).firstMatch
        XCTAssertTrue(biasLabel.waitForExistence(timeout: 10))
        let biasChanged = XCTNSPredicateExpectation(predicate: NSPredicate(format: "label != %@", "밝기 보정: 0.0"), object: biasLabel)
        XCTAssertEqual(XCTWaiter.wait(for: [biasChanged], timeout: 5), .completed)
        let retainedBias = biasLabel.label
        let retainedSlider = String(describing: brightness.value ?? "")
        XCTAssertNotEqual(retainedBias, "밝기 보정: 0.0")
        tapButton("초점 최소화")
        XCTAssertTrue(text("카메라 초점").exists)
        XCTAssertTrue(button("노출 잠금 해제").exists)
        XCTAssertFalse(app.sliders["밝기 조절"].exists)

        // Locked first tap only expands EV. It must not move the focus point.
        let lockedFocusFrame = text("카메라 초점").frame
        tapFocusPoint(0.10, 0.82)
        XCTAssertTrue(text("상태: 노출 잠금").exists)
        XCTAssertTrue(app.sliders["밝기 조절"].waitForExistence(timeout: 10))
        XCTAssertEqual(biasLabel.label, retainedBias)
        let expandedFocusFrame = text("카메라 초점").frame
        XCTAssertEqual(expandedFocusFrame.midX, lockedFocusFrame.midX, accuracy: 1)
        XCTAssertEqual(expandedFocusFrame.midY, lockedFocusFrame.midY, accuracy: 1)
        // Explicit locked refocus preserves the native bias and slider, repeatedly.
        tapFocusPoint(0.88, 0.82)
        XCTAssertTrue(text("상태: 잠금 초점 이동").waitForExistence(timeout: 10))
        let refocusedFrame = text("카메라 초점").frame
        XCTAssertGreaterThan(abs(refocusedFrame.midX - expandedFocusFrame.midX), 20)
        XCTAssertGreaterThan(abs(refocusedFrame.midY - expandedFocusFrame.midY), 20)
        XCTAssertEqual(biasLabel.label, retainedBias)
        XCTAssertEqual(String(describing: brightness.value ?? ""), retainedSlider)
        tapFocusPoint(0.10, 0.82)
        tapFocusPoint(0.88, 0.08)
        XCTAssertEqual(biasLabel.label, retainedBias)
        XCTAssertEqual(String(describing: brightness.value ?? ""), retainedSlider)

        // Temporary input suspension must not forget the still-active camera lock.
        tapButton("조작 잠금")
        // button() requires existence and cannot be used to assert deliberate absence.
        // Native focus controls are hidden while table interaction is suspended.
        let exposureUnlock = app.buttons.matching(NSPredicate(format: "label == %@", "노출 잠금 해제")).firstMatch
        XCTAssertTrue(button("조작 잠금 해제").exists)
        XCTAssertFalse(exposureUnlock.exists)
        tapButton("조작 잠금 해제")
        XCTAssertTrue(button("노출 잠금 해제").waitForExistence(timeout: 10))
        XCTAssertEqual(biasLabel.label, retainedBias)
        XCTAssertFalse(brightness.exists)

        // Exercise real simulator PhotoKit save/recomposition, not just a timer.
        tapButton("이 배치로 촬영 저장")
        XCTAssertTrue(waitForPhotoState("상태: 표 배치·사진 저장 완료"))
        XCTAssertTrue(button("노출 잠금 해제").waitForExistence(timeout: 10))
        XCTAssertEqual(biasLabel.label, retainedBias)
        tapFocusPoint(0.10, 0.82)
        XCTAssertTrue(brightness.waitForExistence(timeout: 10))
        XCTAssertEqual(String(describing: brightness.value ?? ""), retainedSlider)

        tapButton("노출 잠금 해제")
        tapFocusPoint(0.08, 0.08)
        XCTAssertTrue(text("상태: 터치 초점 적용").waitForExistence(timeout: 10))
        XCTAssertTrue(text("밝기 보정: 0.0").exists)
    }

    func testNativePreviewPinchZoomCommitsCompactScale() {
        openLiveTable(reset: true)
        livePreview().pinch(withScale: 2.0, velocity: 1.0)
        XCTAssertTrue(text("상태: 배율 저장 2.0").waitForExistence(timeout: 10), app.debugDescription)
        XCTAssertTrue(text("배율: 2.0").exists)
    }

    func testConfiguredSaveModesAndGalleryAfterRelaunch() { verifyConfiguredModes(merged: false) }

    func testMergedConfiguredSaveModesAndGalleryAfterRelaunch() { verifyConfiguredModes(merged: true) }

    private func verifyConfiguredModes(merged: Bool) {
        for (label, mode, placement) in [("원본", "ORIGINAL_ONLY", "없음"),
                                         ("표 합성", "WATERMARK_ONLY", "BOTTOM_RIGHT"),
                                         ("둘 다", "BOTH", "BOTTOM_RIGHT")] {
            configureCaptureFixture(label, merged: merged)
            XCTAssertTrue(text("모드: \(mode)").exists)
            XCTAssertTrue(text("배치: \(placement)").exists)
            tapButton("촬영 저장")
            XCTAssertTrue(waitForPhotoState("상태: 설정대로 저장 완료"), app.debugDescription)
            XCTAssertTrue(text("다음 번호: 13").exists)
            app.terminate()
            app.launchArguments = configuredArguments(merged: merged)
            app.launch()
            XCTAssertTrue(text("다음 번호: 13").waitForExistence(timeout: 20))
            XCTAssertTrue(text("모드: \(mode)").exists)
            XCTAssertTrue(text("배치: \(placement)").exists)
            tapButton("갤러리 열기")
            XCTAssertTrue(text("갤러리 사진 미리보기").waitForExistence(timeout: 30))
        }
    }

    func testConfiguredPhotoKitSaveRecoversAfterWorkspaceWriteFailure() { verifyConfiguredRecovery(merged: false) }

    func testMergedConfiguredRecoveryAfterWorkspaceFailure() { verifyConfiguredRecovery(merged: true) }

    private func verifyConfiguredRecovery(merged: Bool) {
        for label in ["원본", "표 합성", "둘 다"] {
            configureCaptureFixture(label, merged: merged)
            tapButton("저장 후 복구 대기")
            XCTAssertTrue(waitForPhotoState("상태: PhotoKit 저장·워크스페이스 복구 대기"), app.debugDescription)
            XCTAssertTrue(text("다음 번호: 12").exists)
            XCTAssertFalse(button("촬영 저장").isEnabled)
            app.terminate()
            app.launchArguments = configuredArguments(merged: merged)
            app.launch()
            XCTAssertTrue(text("상태: 재실행 복구 완료").waitForExistence(timeout: 30), app.debugDescription)
            XCTAssertTrue(text("다음 번호: 13").exists)
            XCTAssertTrue(text("대기 기록: false").exists)
            app.terminate()
            app.launch()
            XCTAssertTrue(text("다음 번호: 13").waitForExistence(timeout: 20))
            XCTAssertTrue(text("대기 기록: false").exists)
            tapButton("갤러리 열기")
            XCTAssertTrue(text("갤러리 사진 미리보기").waitForExistence(timeout: 30))
        }
    }

    private func configuredArguments(merged: Bool) -> [String] {
        ["--configured-capture-flow"] + (merged ? ["--merged-capture-fixture"] : [])
    }

    func testCameraControlsStayDisabledWithoutCameraPermission() {
        app.launchArguments = ["--camera-flow", "--deny-camera-permission-for-ui-test"]
        app.launch()
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        XCTAssertFalse(button("촬영").isEnabled)
        XCTAssertTrue(button("촬영 도구").isEnabled)
        XCTAssertFalse(button("직전 촬영 삭제").isEnabled)
        tapButton("촬영 도구")
        XCTAssertFalse(button("자동초점").isEnabled)
        XCTAssertFalse(button("플래시 꺼짐").isEnabled)
        tapButton("앨범 보기")
        XCTAssertTrue(text("갤러리").waitForExistence(timeout: 20))
        tapButton("뒤로가기")
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        XCTAssertFalse(button("촬영").isEnabled)
    }

    func testCameraSettingsPersistThroughNativeModalReturn() {
        app.launchArguments = configuredArguments(merged: true) + ["--reset-configured-fixture", "--deny-camera-permission-for-ui-test"]
        app.launch()
        tapButton("촬영 설정 열기")
        tapCaptureSettingsButton("둘 다", searchDown: true)
        tapCaptureSettingsButton("저장", searchDown: false)
        XCTAssertTrue(text("배치: BOTTOM_RIGHT").waitForExistence(timeout: 20))
        tapButton("촬영 설정 열기")
        tapCaptureSettingsButton("원본", searchDown: true)
        tapCaptureSettingsButton("1:1", searchDown: false)
        tapCaptureSettingsButton("유지", searchDown: true)
        tapCaptureSettingsButton("진동", searchDown: false)
        tapCaptureSettingsButton("저장", searchDown: false)
        tapButton("카메라 열기")
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        tapButton("빠른 값 변경")
        tapPanelText("2행 2열 셀 선택")
        XCTAssertTrue(panelText("2행 2열 · 텍스트").waitForExistence(timeout: 20))
        tapButton("취소")
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        XCTAssertFalse(button("촬영").isEnabled)
        tapButton("촬영 설정")
        tapCaptureSettingsButton("둘 다", searchDown: true)
        XCTAssertTrue(text("표·번호 설정").waitForExistence(timeout: 10),
                      "Applying an option must leave the camera panel open")
        XCTAssertFalse(text("표·번호 설정에서 표 위치를 먼저 저장해 주세요.").exists)
        tapCaptureSettingsButton("닫기", searchDown: false)
        XCTAssertTrue(text("표·번호 설정").waitForNonExistence(timeout: 10))
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        tapButton("뒤로가기")
        XCTAssertTrue(text("모드: BOTH").waitForExistence(timeout: 20))
        XCTAssertTrue(text("배치: BOTTOM_RIGHT").exists)
        XCTAssertTrue(text("다음 번호: 12").exists)
        XCTAssertTrue(text("촬영 확인: 2").exists)
        XCTAssertTrue(text("촬영 진동: false").exists)
        XCTAssertTrue(text("촬영 비율: 1:1").exists)
        app.terminate()
        app.launchArguments = configuredArguments(merged: true)
        app.launch()
        XCTAssertTrue(text("모드: BOTH").waitForExistence(timeout: 20))
        XCTAssertTrue(text("배치: BOTTOM_RIGHT").exists)
        XCTAssertTrue(text("촬영 확인: 2").exists)
        XCTAssertTrue(text("촬영 진동: false").exists)
        XCTAssertTrue(text("촬영 비율: 1:1").exists)
        tapButton("촬영 비율 검증")
        XCTAssertTrue(text("상태: 촬영 비율 검증 완료").waitForExistence(timeout: 20))
    }

    func testCameraOverlayToolsPersistThroughNativeReturn() {
        app.launchArguments = configuredArguments(merged: true) + ["--reset-configured-fixture", "--deny-camera-permission-for-ui-test"]
        app.launch()
        tapButton("촬영 설정 열기")
        tapCaptureSettingsButton("둘 다", searchDown: true)
        tapCaptureSettingsButton("저장", searchDown: false)
        XCTAssertTrue(text("배치: BOTTOM_RIGHT").waitForExistence(timeout: 20))
        tapButton("카메라 열기")
        XCTAssertFalse(button("촬영").isEnabled)
        tapButton("촬영 도구")
        tapButton("격자 표시")
        tapButton("표 숨기기")
        tapButton("보조 셔터 켜기")
        XCTAssertTrue(button("격자 숨기기").exists)
        XCTAssertTrue(button("표 표시").exists)
        XCTAssertTrue(button("보조 셔터 끄기").exists)
        tapButton("촬영 도구")
        XCTAssertTrue(button("보조 셔터").waitForExistence(timeout: 10))
        XCTAssertFalse(button("보조 셔터").isEnabled)
        tapButton("뒤로가기")
        tapButton("카메라 열기")
        tapButton("촬영 도구")
        XCTAssertTrue(button("격자 숨기기").waitForExistence(timeout: 20))
        XCTAssertTrue(button("표 표시").exists)
        XCTAssertTrue(button("보조 셔터 끄기").exists)
    }

    func testGalleryFolderGridViewerAndOriginalResultSwitch() {
        configureCaptureFixture("둘 다", merged: true)
        tapButton("촬영 저장")
        XCTAssertTrue(waitForPhotoState("상태: 설정대로 저장 완료"))
        tapButton("갤러리 열기")
        tapButton("최근 촬영")
        XCTAssertTrue(text("최근 촬영 · 전체 사진 최신순").waitForExistence(timeout: 10))
        XCTAssertTrue(text("사진 2개").waitForExistence(timeout: 10))
        tapButton("뒤로가기")
        tapButton("기본 저장")
        XCTAssertTrue(text("기본 저장 · DZlog 바로 아래 사진").waitForExistence(timeout: 10))
        XCTAssertTrue(text("사진 1개").waitForExistence(timeout: 10))
        tapButton("기본 저장 원본 1개")
        XCTAssertTrue(text("폴더: Pictures/DZlog/original/").waitForExistence(timeout: 10))
        XCTAssertTrue(text("사진 1개").waitForExistence(timeout: 10))
        tapButton("전체 사진")
        let restoredDensity = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "그리드 ")).firstMatch
        XCTAssertTrue(restoredDensity.waitForExistence(timeout: 10))
        tapButton(restoredDensity.label)
        tapPanelText("5열")
        XCTAssertTrue(button("그리드 5열").waitForExistence(timeout: 10))
        tapButton("그리드 5열")
        tapPanelText("2열")
        XCTAssertTrue(button("그리드 2열").waitForExistence(timeout: 10))
        // Compose can merge the image description with the role and filename
        // on the clickable grid cell. The placeholder label is only "사진".
        let thumbnail = app.descendants(matching: .any).matching(NSPredicate(
            format: "label == %@ OR label BEGINSWITH %@", "사진 썸네일", "사진 썸네일\n"
        )).firstMatch
        XCTAssertTrue(thumbnail.waitForExistence(timeout: 30), app.debugDescription)
        // Android atlas A-17: a long press enters selection without opening the viewer.
        thumbnail.press(forDuration: 1.2)
        XCTAssertTrue(text("선택 1개").waitForExistence(timeout: 10))
        XCTAssertTrue(button("선택 취소").exists)
        XCTAssertFalse(text("사진 확대 보기").exists)
        tapButton("선택 취소")
        thumbnail.tap()
        XCTAssertTrue(text("사진 확대 보기").waitForExistence(timeout: 30))
        // The first cell may be the original; restore the result selection
        // before exercising the explicit original/result companion controls.
        if app.buttons.matching(NSPredicate(format: "label == %@", "결과 보기")).firstMatch.exists {
            tapButton("결과 보기")
            XCTAssertTrue(text("사진 확대 보기").waitForExistence(timeout: 30))
        }
        tapButton("사진 목록")
        XCTAssertTrue(text("갤러리 사진 미리보기").waitForExistence(timeout: 30))
        tapPanelText("갤러리 사진 미리보기")
        XCTAssertTrue(text("사진 확대 보기").waitForExistence(timeout: 30))
        XCTAssertTrue(text("배율 100%").waitForExistence(timeout: 10))
        tapButton("사진 정보")
        XCTAssertTrue(text("사진 종류: 결과").waitForExistence(timeout: 10))
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH %@", "표시 이미지 크기:")).firstMatch.exists)
        tapButton("닫기")
        if button("다음 사진").isEnabled {
            text("사진 확대 보기").swipeLeft()
        } else {
            text("사진 확대 보기").swipeRight()
        }
        XCTAssertTrue(button("결과 보기").waitForExistence(timeout: 20), "Fitted swipe must navigate to the original")
        tapButton("결과 보기")
        XCTAssertTrue(text("배율 100%").waitForExistence(timeout: 20))
        tapButton("확대")
        XCTAssertTrue(text("배율 150%").waitForExistence(timeout: 10))
        tapButton("조작부 숨김")
        XCTAssertTrue(button("조작부 표시").waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "label == %@", "사진 삭제")).firstMatch.exists)
        tapButton("조작부 표시")
        XCTAssertTrue(text("배율 150%").waitForExistence(timeout: 10), "Hiding chrome must not reset zoom")
        tapButton("화면 맞춤")
        XCTAssertTrue(text("배율 100%").waitForExistence(timeout: 10))
        tapPanelText("사진 확대 보기")
        XCTAssertTrue(button("조작부 표시").waitForExistence(timeout: 10), "Single image tap must toggle chrome")
        tapButton("조작부 표시")
        XCTAssertFalse(button("축소").isEnabled)
        tapButton("확대")
        XCTAssertTrue(text("배율 150%").waitForExistence(timeout: 10))
        tapButton("사진 다시 읽기")
        XCTAssertTrue(text("배율 100%").waitForExistence(timeout: 30), "Retry must actually reload even with unchanged metadata")
        XCTAssertTrue(text("사진 복원 완료").waitForExistence(timeout: 30))
        XCTAssertTrue(text("사진 확대 보기").waitForExistence(timeout: 30))
        tapButton("확대")
        tapButton("원본 보기")
        XCTAssertTrue(button("결과 보기").waitForExistence(timeout: 20))
        XCTAssertTrue(text("배율 100%").waitForExistence(timeout: 30), "A companion must start fitted, not inherit zoom")
        tapButton("결과 보기")
        tapButton("사진 목록")
        tapButton("사진 정렬 · 최신순")
        tapPanelText("오래된순")
        XCTAssertTrue(button("사진 정렬 · 오래된순").waitForExistence(timeout: 10))
        tapButton("사진 정렬 · 오래된순")
        tapPanelText("이름순")
        XCTAssertTrue(button("사진 정렬 · 이름순").waitForExistence(timeout: 10))
        tapPanelText("갤러리 사진 미리보기")
        XCTAssertTrue(button("원본 보기").waitForExistence(timeout: 20), "Sort must keep selected result identity")
        tapButton("사진 목록")
        tapButton("선택")
        tapButton("전체 선택")
        XCTAssertTrue(text("선택 2개").waitForExistence(timeout: 10))
        tapButton("선택 해제")
        XCTAssertTrue(text("선택 0개").waitForExistence(timeout: 10))
        XCTAssertFalse(button("선택 삭제").isEnabled)
        XCTAssertFalse(button("선택 공유").isEnabled)
        tapButton("전체 선택")
        XCTAssertTrue(button("선택 공유").isEnabled)
        tapButton("선택 삭제")
        XCTAssertTrue(text("사진 삭제").waitForExistence(timeout: 10))
        tapButton("취소")
        XCTAssertTrue(text("선택 2개").waitForExistence(timeout: 10))
        tapButton("선택 이동")
        XCTAssertTrue(text("이동할 위치 선택").waitForExistence(timeout: 10))
        tapButton("취소")
        tapButton("좋아요만 보기")
        XCTAssertTrue(text("선택 0개").waitForExistence(timeout: 10), "Hidden selections must not survive a filter change")
        tapButton("전체 보기")
        XCTAssertTrue(text("사진 2개").waitForExistence(timeout: 10), "Cancelling batch dialogs must keep both Photos assets")
        XCTAssertTrue(text("선택 0개").exists)
        tapButton("선택 취소")
        XCTAssertTrue(button("그리드 2열").waitForExistence(timeout: 10), "Viewer and selection must retain density")
        tapButton("폴더 보기")
        XCTAssertTrue(text("폴더: Pictures/DZlog/").waitForExistence(timeout: 20))
        tapButton("목록 보기")
        XCTAssertTrue(button("그리드 보기").exists)
        tapButton("그리드 보기")
        XCTAssertTrue(button("그리드 2열").waitForExistence(timeout: 10), "List/grid toggle must retain density")
        tapButton("전체 사진")
        tapButton("뒤로가기")
        XCTAssertTrue(text("다음 번호: 13").waitForExistence(timeout: 20))
        app.terminate()
        app.launchArguments = configuredArguments(merged: true)
        app.launch()
        tapButton("갤러리 열기")
        XCTAssertTrue(button("그리드 2열").waitForExistence(timeout: 20), "Grid preference must survive relaunch")
    }

    func testGalleryMoveKeepsViewerOrderUntilReopened() {
        configureCaptureFixture("둘 다", merged: true)
        tapButton("촬영 저장")
        XCTAssertTrue(waitForPhotoState("상태: 설정대로 저장 완료"))
        tapButton("갤러리 열기")
        tapButton("폴더 보기")
        tapButton("폴더 생성")
        tapPanelText("폴더 이름")
        typeWithVisibleKeyboard("moved")
        tapButton("저장")
        XCTAssertTrue(text("폴더: Pictures/DZlog/moved/").waitForExistence(timeout: 20))
        tapButton("전체 사진")
        tapPanelText("갤러리 사진 미리보기")
        XCTAssertTrue(text("사진 확대 보기").waitForExistence(timeout: 30))
        if text("결과 보기").exists { tapButton("결과 보기") }
        XCTAssertTrue(text("원본 보기").waitForExistence(timeout: 10))
        let move = app.buttons["사진 이동"]
        if !move.exists || move.frame.isEmpty || !app.frame.contains(move.frame) {
            let rowY = button("조작부 숨김").frame.midY
            let origin = app.coordinate(withNormalizedOffset: .zero)
            origin.withOffset(CGVector(dx: app.frame.width * 0.9, dy: rowY)).press(
                forDuration: 0.1, thenDragTo: origin.withOffset(CGVector(dx: app.frame.width * 0.1, dy: rowY)))
        }
        tapButton("사진 이동")
        XCTAssertTrue(text("저장 폴더").waitForExistence(timeout: 20))
        XCTAssertFalse(button("이 위치로 이동").isEnabled)
        tapButton("원본사진 1장")
        XCTAssertFalse(button("이 위치로 이동").isEnabled)
        tapButton("상위 폴더")
        tapPanelText("moved")
        tapButton("이 위치로 이동")
        XCTAssertFalse(button("다음").isEnabled)
        tapButton("사진만")
        tapButton("다음")
        XCTAssertTrue(text("결과사진 1장 · Pictures/DZlog/moved/").waitForExistence(timeout: 10))
        tapButton("이동")
        XCTAssertTrue(text("사진을 이동했습니다.").waitForExistence(timeout: 20))
        tapButton("사진 다시 읽기")
        XCTAssertTrue(text("사진 복원 완료").waitForExistence(timeout: 30))
        tapButton("사진 정보")
        XCTAssertTrue(text("사진 종류: 원본").waitForExistence(timeout: 10))
        tapButton("닫기")
        XCTAssertFalse(button("다음 사진").isEnabled)
        XCTAssertFalse(button("이전 사진").isEnabled)
        tapButton("사진 목록")
        XCTAssertTrue(text("사진 2개").waitForExistence(timeout: 10))
        tapButton("폴더 보기")
        tapButton("moved")
        XCTAssertTrue(text("사진 1개").waitForExistence(timeout: 10))
        tapPanelText("갤러리 사진 미리보기")
        tapButton("사진 정보")
        XCTAssertTrue(text("사진 종류: 결과").waitForExistence(timeout: 10))
        XCTAssertTrue(text("폴더: Pictures/DZlog/moved/").exists)
        tapButton("닫기")
        tapButton("사진 목록")
        tapButton("폴더 생성")
        tapPanelText("폴더 이름")
        typeWithVisibleKeyboard("pair")
        tapButton("저장")
        XCTAssertTrue(text("폴더: Pictures/DZlog/moved/pair/").waitForExistence(timeout: 20))
        tapButton("상위 폴더")
        tapButton("선택")
        tapButton("전체 선택")
        XCTAssertTrue(text("선택 1개").waitForExistence(timeout: 10))
        tapButton("선택 이동")
        tapPanelText("moved")
        tapPanelText("pair")
        tapButton("이 위치로 이동")
        tapButton("사진만")
        tapButton("다음")
        tapButton("이동 선택 변경")
        tapButton("이동 위치 변경")
        tapButton("DZlog")
        tapPanelText("moved")
        tapPanelText("pair")
        tapButton("이 위치로 이동")
        tapButton("원본 포함")
        tapButton("다음")
        XCTAssertTrue(text("2개 폴더에서 선택").waitForExistence(timeout: 10))
        XCTAssertTrue(text("결과사진 1장 · Pictures/DZlog/moved/pair/").exists)
        XCTAssertTrue(text("원본사진 1장 · Pictures/DZlog/moved/pair/original/").exists)
        tapButton("취소")
        XCTAssertTrue(text("선택 1개").waitForExistence(timeout: 10))
        tapButton("선택 이동")
        tapPanelText("moved")
        tapPanelText("pair")
        tapButton("이 위치로 이동")
        tapButton("원본 포함")
        tapButton("다음")
        tapButton("이동")
        XCTAssertTrue(text("폴더: Pictures/DZlog/moved/pair/").waitForExistence(timeout: 20))
        XCTAssertTrue(text("사진 1개").waitForExistence(timeout: 10))
        tapButton("original")
        XCTAssertTrue(text("폴더: Pictures/DZlog/moved/pair/original/").waitForExistence(timeout: 10))
        XCTAssertTrue(text("사진 1개").waitForExistence(timeout: 10))

        // Leave only the linked original in pair, then reopen the parent from the root.
        tapButton("상위 폴더")
        tapButton("선택")
        tapButton("전체 선택")
        XCTAssertTrue(text("선택 1개").waitForExistence(timeout: 10))
        tapButton("선택 이동")
        XCTAssertTrue(text("저장 폴더").waitForExistence(timeout: 20))
        tapButton("이 위치로 이동")
        tapButton("사진만")
        tapButton("다음")
        tapButton("이동")
        XCTAssertTrue(text("폴더: Pictures/DZlog/").waitForExistence(timeout: 20))
        tapButton("moved")
        tapButton("pair")
        XCTAssertTrue(text("폴더: Pictures/DZlog/moved/pair/").waitForExistence(timeout: 10))
        XCTAssertTrue(text("사진 1개").waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "label == %@", "original")).firstMatch.exists)
        tapPanelText("갤러리 사진 미리보기")
        tapButton("사진 정보")
        XCTAssertTrue(text("사진 종류: 원본").waitForExistence(timeout: 10))
        XCTAssertTrue(text("폴더: Pictures/DZlog/moved/pair/original/").exists)
    }

    func testFolderRenamePreviewsPathRejectsCollisionAndPreservesCancellation() {
        configureCaptureFixture("둘 다", merged: true)
        tapButton("갤러리 열기")
        tapButton("폴더 보기")
        let root = "Pictures/DZlog/"
        for name in ["aa", "a"] {
            tapButton("폴더 생성")
            tapPanelText("폴더 이름")
            typeWithVisibleKeyboard(name)
            tapButton("저장")
            XCTAssertTrue(text("폴더: " + root + name + "/").waitForExistence(timeout: 20))
            if name == "aa" { tapButton("상위 폴더") }
        }
        tapButton("이름 변경")
        XCTAssertTrue(text("현재 경로").waitForExistence(timeout: 10))
        XCTAssertTrue(text("변경 후 경로").exists)
        tapPanelText("새 폴더 이름")
        typeWithVisibleKeyboard("a")
        XCTAssertTrue(text("같은 이름의 폴더가 이미 있습니다.").waitForExistence(timeout: 10))
        XCTAssertFalse(button("이름 변경").isEnabled)
        typeWithVisibleKeyboard("a")
        XCTAssertTrue(text(root + "aaa/").waitForExistence(timeout: 10))
        tapButton("취소")
        XCTAssertTrue(text("폴더: " + root + "a/").waitForExistence(timeout: 10))
        tapButton("이름 변경")
        tapPanelText("새 폴더 이름")
        typeWithVisibleKeyboard("aa")
        XCTAssertTrue(text(root + "aaa/").waitForExistence(timeout: 10))
        tapButton("이름 변경")
        XCTAssertTrue(text("폴더: " + root + "aaa/").waitForExistence(timeout: 20))
        app.terminate()
        app.launchArguments = configuredArguments(merged: true)
        app.launch()
        tapButton("갤러리 열기")
        tapButton("폴더 보기")
        tapButton("aaa")
        XCTAssertTrue(text("폴더: " + root + "aaa/").waitForExistence(timeout: 10))
    }

    func testFolderMoveChoosesDestinationBeforeCommittingSubtree() {
        configureCaptureFixture("둘 다", merged: true)
        tapButton("갤러리 열기")
        tapButton("폴더 보기")
        let root = "Pictures/DZlog/"
        func createFolder(_ name: String, under parent: String) {
            tapButton("폴더 생성")
            tapPanelText("폴더 이름")
            typeWithVisibleKeyboard(name)
            tapButton("저장")
            XCTAssertTrue(text("폴더: " + parent + name + "/").waitForExistence(timeout: 20))
        }
        createFolder("target", under: root)
        tapButton("상위 폴더")
        createFolder("source", under: root)
        createFolder("child", under: root + "source/")
        tapButton("상위 폴더")

        tapButton("폴더 이동")
        XCTAssertTrue(text("이미 같은 위치에 있는 폴더입니다.").waitForExistence(timeout: 20))
        XCTAssertFalse(button("이 위치로 이동").isEnabled)
        tapPanelText("target")
        tapButton("이 위치로 이동")
        XCTAssertTrue(text(root + "target/source/").waitForExistence(timeout: 10))
        tapButton("이동 위치 변경")
        tapButton("DZlog")
        tapButton("취소")
        XCTAssertTrue(text(root + "target/source/").waitForExistence(timeout: 10))
        tapButton("취소")
        XCTAssertTrue(text("폴더: " + root + "source/").waitForExistence(timeout: 10))
        XCTAssertTrue(button("child").exists)

        tapButton("폴더 이동")
        tapPanelText("target")
        tapButton("이 위치로 이동")
        tapButton("이동")
        XCTAssertTrue(text("폴더: " + root + "target/source/").waitForExistence(timeout: 20))
        tapButton("child")
        XCTAssertTrue(text("폴더: " + root + "target/source/child/").waitForExistence(timeout: 10))
        app.terminate()
        app.launchArguments = configuredArguments(merged: true)
        app.launch()
        tapButton("갤러리 열기")
        tapButton("폴더 보기")
        XCTAssertTrue(button("target").exists)
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "label == %@", "source")).firstMatch.exists)
        tapButton("target")
        tapButton("source")
        tapButton("child")
        XCTAssertTrue(text("폴더: " + root + "target/source/child/").waitForExistence(timeout: 10))
    }

    func testGalleryFavoriteAndEmptyFolderManagementPersist() {
        configureCaptureFixture("둘 다", merged: true)
        tapButton("촬영 저장")
        XCTAssertTrue(waitForPhotoState("상태: 설정대로 저장 완료"))
        tapButton("갤러리 열기")
        XCTAssertTrue(text("갤러리").waitForExistence(timeout: 10))
        let thumbnail = app.descendants(matching: .any).matching(NSPredicate(
            format: "label == %@ OR label BEGINSWITH %@", "사진 썸네일", "사진 썸네일\n"
        )).firstMatch
        XCTAssertTrue(thumbnail.waitForExistence(timeout: 30), app.debugDescription)
        thumbnail.tap()
        tapButton("☆ 즐겨찾기")
        XCTAssertTrue(button("★ 즐겨찾기").waitForExistence(timeout: 10))

        // Leave the non-favorite companion selected, then filter the list.
        // The preview must select the remaining favorite rather than disappear.
        tapButton(app.buttons.matching(NSPredicate(format: "label == %@", "원본 보기")).firstMatch.exists ? "원본 보기" : "결과 보기")
        XCTAssertTrue(button("☆ 즐겨찾기").waitForExistence(timeout: 10))
        tapButton("사진 목록")
        tapButton("좋아요만 보기")
        XCTAssertTrue(text("사진 1개").waitForExistence(timeout: 10))
        XCTAssertTrue(text("갤러리 사진 미리보기").waitForExistence(timeout: 30))
        tapPanelText("갤러리 사진 미리보기")
        XCTAssertTrue(button("★ 즐겨찾기").waitForExistence(timeout: 10))
        XCTAssertFalse(button("이전 사진").isEnabled)
        XCTAssertFalse(button("다음 사진").isEnabled)
        tapButton(app.buttons.matching(NSPredicate(format: "label == %@", "원본 보기")).firstMatch.exists ? "원본 보기" : "결과 보기")
        XCTAssertTrue(button("☆ 즐겨찾기").waitForExistence(timeout: 10))
        // An out-of-filter companion must not expose unrelated library navigation.
        XCTAssertFalse(button("이전 사진").isEnabled)
        XCTAssertFalse(button("다음 사진").isEnabled)
        tapButton(app.buttons.matching(NSPredicate(format: "label == %@", "원본 보기")).firstMatch.exists ? "원본 보기" : "결과 보기")
        XCTAssertTrue(button("★ 즐겨찾기").waitForExistence(timeout: 10))

        app.terminate()
        app.launchArguments = configuredArguments(merged: true)
        app.launch()
        tapButton("갤러리 열기")
        tapButton("좋아요만 보기")
        let favoriteThumbnail = app.descendants(matching: .any).matching(NSPredicate(
            format: "label == %@ OR label BEGINSWITH %@", "사진 썸네일", "사진 썸네일\n"
        )).firstMatch
        XCTAssertTrue(favoriteThumbnail.waitForExistence(timeout: 30), app.debugDescription)
        tapButton("폴더 보기")
        tapButton("폴더 생성")
        XCTAssertFalse(button("저장").isEnabled, "An empty folder name must not be saved")
        tapPanelText("폴더 이름")
        typeWithVisibleKeyboard("qa")
        XCTAssertTrue(button("저장").isEnabled, "Typing a valid folder name must enable save")
        tapButton("저장")
        XCTAssertTrue(text("폴더: Pictures/DZlog/qa/").waitForExistence(timeout: 20), app.debugDescription)
        XCTAssertTrue(text("사진이 없습니다.").waitForExistence(timeout: 10))
        XCTAssertFalse(text("즐겨찾기한 사진이 없습니다.").exists)
        tapButton("전체 사진")
        tapButton("폴더 보기")
        tapButton("폴더 · 전체")
        tapPanelText("사진 있음")
        XCTAssertTrue(button("폴더 · 사진 있음").waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "label == %@", "qa")).firstMatch.exists)
        tapButton("폴더 · 사진 있음")
        tapPanelText("빈 폴더")
        tapButton("폴더순 · 이름순")
        tapPanelText("최근 사진순")
        XCTAssertTrue(button("폴더순 · 최근 사진순").waitForExistence(timeout: 10))
        tapButton("qa")
        XCTAssertTrue(text("폴더: Pictures/DZlog/qa/").waitForExistence(timeout: 20))
        tapButton("폴더 삭제")
        tapButton("삭제")
        XCTAssertTrue(text("폴더: Pictures/DZlog/").waitForExistence(timeout: 20))
        tapButton("전체 사진")
        let allPhotosFavoriteThumbnail = app.descendants(matching: .any).matching(NSPredicate(
            format: "label == %@ OR label BEGINSWITH %@", "사진 썸네일", "사진 썸네일\n"
        )).firstMatch
        XCTAssertTrue(allPhotosFavoriteThumbnail.waitForExistence(timeout: 30))
        allPhotosFavoriteThumbnail.tap()
        tapButton("★ 즐겨찾기")
        XCTAssertTrue(button("☆ 즐겨찾기").waitForExistence(timeout: 10))
        tapButton("사진 목록")
        XCTAssertTrue(text("사진 0개").waitForExistence(timeout: 10))
        XCTAssertTrue(text("즐겨찾기 사진이 없습니다.").waitForExistence(timeout: 10))
        XCTAssertTrue(text("즐겨찾기한 사진이 없습니다.").waitForExistence(timeout: 10))
        XCTAssertFalse(text("사진이 없습니다.").exists)
        XCTAssertFalse(text("갤러리 사진 미리보기").exists)
        tapButton("전체 보기")
        XCTAssertTrue(text("사진 2개").waitForExistence(timeout: 10))
        XCTAssertFalse(text("즐겨찾기한 사진이 없습니다.").exists)
        XCTAssertFalse(text("사진이 없습니다.").exists)
        XCTAssertTrue(text("갤러리 사진 미리보기").waitForExistence(timeout: 30))
    }

    func testMergedRasterPixelsAndMalformedSpans() {
        app.launchArguments = configuredArguments(merged: true) + ["--reset-configured-fixture"]
        app.launch()
        tapButton("병합 렌더 검증")
        XCTAssertTrue(text("상태: 병합 픽셀·오류 차단 검증 완료").waitForExistence(timeout: 20), app.debugDescription)
    }

    func testExifRasterIsUprightAndOriginalRemainsUnchanged() {
        app.launchArguments = configuredArguments(merged: false) + ["--reset-configured-fixture"]
        app.launch()
        tapButton("사진 방향 검증")
        XCTAssertTrue(text("상태: 사진 방향 검증 완료").waitForExistence(timeout: 20), app.debugDescription)
    }

    private func configureCaptureFixture(_ modeLabel: String, merged: Bool) {
        if app.state != .notRunning { app.terminate() }
        app.launchArguments = configuredArguments(merged: merged) + ["--reset-configured-fixture"]
        app.launch()
        if merged { XCTAssertTrue(text("병합 표 사용 중").waitForExistence(timeout: 20)) }
        tapButton("촬영 설정 열기")
        tapCaptureSettingsButton(modeLabel, searchDown: true)
        tapCaptureSettingsButton("저장", searchDown: false)
        XCTAssertTrue(text("상태: 설정 복원 완료").waitForExistence(timeout: 20))
    }

    // Capture settings has grown beyond one viewport. Exercise real scrolling
    // instead of treating an offscreen Compose AX node (zero frame) as tappable.
    // Save stays at the top, so callers search upward after choosing a lower row.
    private func tapCaptureSettingsButton(_ label: String, searchDown: Bool) {
        XCTAssertTrue(text("촬영 설정").waitForExistence(timeout: 20), "Capture settings did not open")
        let matches = app.buttons.matching(NSPredicate(format: "label == %@", label))
        func renderedLabelFrame() throws -> CGRect? {
            let request = VNRecognizeTextRequest()
            request.recognitionLevel = .accurate
            request.usesCPUOnly = true
            request.recognitionLanguages = ["ko-KR", "en-US"]
            request.usesLanguageCorrection = false
            let image = try XCTUnwrap(app.screenshot().image.cgImage)
            try VNImageRequestHandler(cgImage: image, options: [:]).perform([request])
            func normalized(_ value: String) -> String {
                value.components(separatedBy: .whitespacesAndNewlines).joined()
            }
            var targets = (request.results ?? []).filter {
                $0.topCandidates(1).first.map { normalized($0.string) == normalized(label) } ?? false
            }
            // The compact panel contains both a Save action and a Save section title.
            // Match the action only on the same rendered header row as the panel title;
            // never pick an arbitrary OCR occurrence or trust a stale scrolled AX frame.
            if label == "저장" {
                let titles = (request.results ?? []).filter {
                    $0.topCandidates(1).first.map { normalized($0.string) == "촬영설정" } ?? false
                }
                guard titles.count == 1, let title = titles.first else { return nil }
                targets = targets.filter { target in
                    abs(target.boundingBox.midY - title.boundingBox.midY)
                        <= max(target.boundingBox.height, title.boundingBox.height)
                }
            }
            // An ambiguous or absent rendered label is not permission to guess a tap.
            guard targets.count == 1, let target = targets.first else { return nil }
            let box = target.boundingBox
            let viewport = app.frame
            let frame = CGRect(x: viewport.minX + box.minX * viewport.width,
                               y: viewport.minY + (1 - box.maxY) * viewport.height,
                               width: box.width * viewport.width, height: box.height * viewport.height)
            guard !frame.isEmpty && viewport.contains(frame) else { return nil }
            return frame
        }
        // Avoid a one-second predicate waiter around synchronous AX queries: its
        // first poll can occur at the deadline, discarding a valid result. Also,
        // Compose can retain a stale AX frame after scrolling. Require the exact
        // rendered label in the current screenshot, plus an enabled semantic button.
        for downward in [searchDown, !searchDown] {
            for attempt in 0...6 {
                do {
                    if let frame = try renderedLabelFrame() {
                        let element = matches.firstMatch
                        XCTAssertTrue(element.exists, "Rendered capture setting has no button: \(label)")
                        XCTAssertTrue(element.isEnabled, "Capture setting is disabled: \(label)")
                        guard element.exists && element.isEnabled else { return }
                        XCTAssertFalse(frame.isEmpty, "Capture setting has no visible bounds: \(label)")
                        XCTAssertTrue(app.frame.contains(frame), "Capture setting outside screen: \(label)")
                        print("CAPTURE_SETTING_TOUCH: \(label) rendered=\(frame) ax=\(element.frame)")
                        app.coordinate(withNormalizedOffset: .zero)
                            .withOffset(CGVector(dx: frame.midX - app.frame.minX,
                                                 dy: frame.midY - app.frame.minY)).tap()
                        return
                    }
                } catch {
                    XCTFail("Cannot verify rendered capture setting \(label): \(error)")
                    return
                }
                if attempt < 6 {
                    let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: downward ? 0.75 : 0.3))
                    let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: downward ? 0.3 : 0.75))
                    start.press(forDuration: 0.05, thenDragTo: end)
                }
            }
        }
        XCTFail("Capture setting was not visible after bounded bidirectional scrolling: \(label)\n\(app.debugDescription)")
    }

    // Observe completion and the asynchronous system prompt together. A fixed
    // short prompt wait can expire before PhotoKit asks, while a long fixed wait
    // delays every already-authorized save and still misses later dialogs.
    private func waitForPhotoState(_ label: String, timeout: TimeInterval = 60) -> Bool {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            let alert = springboard.alerts.firstMatch
            if alert.exists {
                for name in ["Allow Full Access", "Allow Access to All Photos", "Allow Access to All Photos and Videos"] {
                    if alert.buttons[name].exists { alert.buttons[name].tap(); break }
                }
            }
            if text(label).exists { return true }
            Thread.sleep(forTimeInterval: 0.25)
        }
        return text(label).exists
    }


    func testMediaFixturePhotoKitSaveAndRelaunch() {
        app.launchArguments = ["--media-flow"]
        app.launch()
        XCTAssertTrue(text("DZlog M3-A PhotoKit 검증").waitForExistence(timeout: 15))
        tapButton("테스트 사진 저장")
        XCTAssertTrue(waitForPhotoState("저장: 저장 완료: 2개"))
        XCTAssertTrue(text("조회: 사진 복원 완료").waitForExistence(timeout: 20))
        XCTAssertTrue(text("PhotoKit 복원 이미지").exists)
        app.terminate()
        app.launchArguments = ["--media-flow"]
        app.launch()
        XCTAssertTrue(text("DZlog M3-A PhotoKit 검증").waitForExistence(timeout: 15))
        XCTAssertTrue(text("조회: 사진 복원 완료").waitForExistence(timeout: 30))
        XCTAssertTrue(text("PhotoKit 복원 이미지").exists)
        app.terminate()
        app.launchArguments = ["--home-navigation", "--deny-camera-permission-for-ui-test"]
        app.launch()
        enterHomeAfterOptionalOnboarding()
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(
            format: "label BEGINSWITH %@", "오늘 "
        )).firstMatch.waitForExistence(timeout: 20), app.debugDescription)
        XCTAssertTrue(text("DZlog/M3A-PhotoKit").exists)
        XCTAssertFalse(text("DZlog 저장 사진").exists)
        let recent = text("최근 사진")
        XCTAssertTrue(recent.waitForExistence(timeout: 20))
        recent.tap()
        XCTAssertTrue(text("폴더: Pictures/DZlog/M3A-PhotoKit/").waitForExistence(timeout: 15))
        tapButton("폴더 삭제")
        let folderCounts = app.descendants(matching: .any).matching(NSPredicate(
            format: "label MATCHES %@", "결과 사진 [1-9][0-9]*장 · 원본 사진 [1-9][0-9]*장"
        )).firstMatch
        XCTAssertTrue(folderCounts.waitForExistence(timeout: 15), app.debugDescription)
        tapButton("취소")
        XCTAssertTrue(text("폴더: Pictures/DZlog/M3A-PhotoKit/").exists)
        tapButton("뒤로가기")
        XCTAssertTrue(text("앨범 보기").waitForExistence(timeout: 15))
        tapButton("앨범 보기")
        XCTAssertTrue(text("갤러리").waitForExistence(timeout: 15))
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label MATCHES %@", "사진 ([2-9]|[1-9][0-9]+)개")).firstMatch.waitForExistence(timeout: 15), "Gallery count mismatch after fixture save. UI hierarchy: \(app.debugDescription)")
        XCTAssertTrue(text("갤러리 사진 미리보기").waitForExistence(timeout: 30))
        tapButton("뒤로가기")
        XCTAssertTrue(text("최근 사진").waitForExistence(timeout: 15))
        // After a camera round trip the home and saved assets must remain readable.
        tapButton("촬영 시작")
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        tapButton("뒤로가기")
        XCTAssertTrue(text("최근 사진").waitForExistence(timeout: 20))
        tapButton("앨범 보기")
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label MATCHES %@", "사진 ([2-9]|[1-9][0-9]+)개")).firstMatch.waitForExistence(timeout: 20))
    }

    // Synthetic JPEG exercises the same iOS workspace/PhotoKit coordinator as the home camera.
    // No simulator camera or production preference key is used.
    func testCaptureWorkspaceCommitAndRelaunch() {
        app.launchArguments = ["--capture-workspace-flow"]
        app.launch()
        XCTAssertTrue(text("iOS M3-A 워크스페이스 저장 검증").waitForExistence(timeout: 15))
        tapButton("테스트 촬영 저장")
        XCTAssertTrue(waitForPhotoState("저장 상태: 원본 저장·워크스페이스 확정"))
        XCTAssertTrue(text("다음 촬영 번호: 13").exists)
        XCTAssertTrue(text("자동번호 셀: 13").exists)

        app.terminate()
        app.launchArguments = ["--capture-workspace-flow"]
        app.launch()
        XCTAssertTrue(text("다음 촬영 번호: 13").waitForExistence(timeout: 20))
        XCTAssertTrue(text("자동번호 셀: 13").exists)
    }

    // In the simulator, a synthetic frame drives the real two-role PhotoKit
    // transaction and workspace commit, then verifies persistent counter state.
    func testBothModeCompositedJpegPhotoKitSaveAndRelaunch() {
        app.launchArguments = ["--capture-workspace-flow"]
        app.launch()
        XCTAssertTrue(text("iOS M3-A 워크스페이스 저장 검증").waitForExistence(timeout: 15))
        tapButton("BOTH 합성 실제 저장 검증")
        XCTAssertTrue(waitForPhotoState("저장 상태: BOTH 합성·원본 저장·번호 확정"))
        XCTAssertTrue(text("다음 촬영 번호: 13").exists)
        XCTAssertTrue(text("자동번호 셀: 13").exists)
        app.terminate()
        app.launchArguments = ["--capture-workspace-flow"]
        app.launch()
        XCTAssertTrue(text("다음 촬영 번호: 13").waitForExistence(timeout: 20))
        XCTAssertTrue(text("자동번호 셀: 13").exists)
    }

    // BOTH must reject missing rendered bytes without committing a counter or a pending journal.
    func testBothModeMissingRenderedJpegIsRejected() {
        app.launchArguments = ["--capture-workspace-flow"]
        app.launch()
        XCTAssertTrue(text("iOS M3-A 워크스페이스 저장 검증").waitForExistence(timeout: 15))
        tapButton("BOTH 합성 누락 검증")
        XCTAssertTrue(text("저장 상태: 합성 이미지 누락 차단·번호 유지").waitForExistence(timeout: 20))
        XCTAssertTrue(text("다음 촬영 번호: 12").exists)
    }

    func testCaptureRasterCompositorProducesJpegAndRejectsMismatchedOverlay() {
        app.launchArguments = ["--capture-workspace-flow"]
        app.launch()
        XCTAssertTrue(text("iOS M3-A 워크스페이스 저장 검증").waitForExistence(timeout: 15))
        tapButton("표 합성 어댑터 검증")
        XCTAssertTrue(text("저장 상태: 합성 JPEG 생성·크기 검증 완료").waitForExistence(timeout: 20))
    }

    func testCaptureResolvedTableOverlayProducesJpeg() {
        app.launchArguments = ["--capture-workspace-flow"]
        app.launch()
        XCTAssertTrue(text("iOS M3-A 워크스페이스 저장 검증").waitForExistence(timeout: 15))
        tapButton("표 데이터 합성 검증")
        XCTAssertTrue(text("저장 상태: 표 데이터 오버레이 합성 완료").waitForExistence(timeout: 20))
    }

    // Denied permission must never start a camera session or allow capturing.
    // This test-only argument forces denial; real OS permission prompts need device QA.
    func testCameraPermissionDeniedBlocksCapture() {
        app.launchArguments = ["--camera-flow", "--deny-camera-permission-for-ui-test"]
        app.launch()
        // The aligned camera chrome no longer renders a redundant screen title.
        XCTAssertTrue(app.buttons["촬영 도구"].waitForExistence(timeout: 20))
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        XCTAssertTrue(button("iPhone 설정 열기").isEnabled)
        XCTAssertTrue(text("이번 촬영: 0장").exists)
        XCTAssertFalse(button("촬영").isEnabled)
    }

    // Real home entry uses the Swift permission gate and preserves the home VC on return.
    func testHomeCameraPermissionDeniedEntryAndReturn() {
        app.launchArguments = ["--home-navigation", "--deny-camera-permission-for-ui-test"]
        app.launch()
        enterHomeAfterOptionalOnboarding()
        XCTAssertTrue(button("촬영 시작").isEnabled)
        tapButton("촬영 시작")
        // The aligned camera chrome no longer renders a redundant screen title.
        XCTAssertTrue(app.buttons["촬영 도구"].waitForExistence(timeout: 20))
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        XCTAssertFalse(button("촬영").isEnabled)
        tapButton("뒤로가기")
        XCTAssertTrue(button("촬영 시작").waitForExistence(timeout: 20))
        XCTAssertTrue(text("최근 촬영").exists) // Home content is restored; counter text is not an exposed AX node.
    }

    // These tests cover home/media behavior; complete first-launch onboarding via its real skip action.
    private func enterHomeAfterOptionalOnboarding() {
        let home = app.buttons.matching(NSPredicate(format: "label == %@", "촬영 시작")).firstMatch
        let skip = app.buttons.matching(NSPredicate(format: "label == %@", "건너뛰기")).firstMatch
        let ready = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            home.exists || skip.exists
        }, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [ready], timeout: 20), .completed)
        if skip.exists { tapButton("건너뛰기") }
        XCTAssertTrue(home.waitForExistence(timeout: 20))
    }

    private func text(_ label: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    private func button(_ label: String) -> XCUIElement {
        // Overlay and underlying editor can both publish the same Back label.
        // Taps use screen bounds; the visible overlay receives the actual touch.
        let element = app.buttons.matching(NSPredicate(format: "label == %@", label)).firstMatch
        XCTAssertTrue(element.waitForExistence(timeout: 20), "Missing button: \(label)")
        XCTAssertFalse(element.frame.isEmpty, "Button has no visible bounds: \(label)")
        // Native screen bounds check is a basic Safe Area sanity check, not full layout approval.
        XCTAssertTrue(app.frame.contains(element.frame), "Button outside screen: \(label)")
        return element
    }

    private func tapButton(_ label: String) {
        let frame = button(label).frame
        // Compose 1.7 popup AX hit points are unreliable. Send a real touch at its visible frame;
        // each caller verifies the resulting dismissal/selection, not just event synthesis.
        app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: frame.midX, dy: frame.midY)).tap()
    }

    private func tapScrollableButton(_ label: String) {
        for _ in 0..<5 {
            let element = app.buttons.matching(NSPredicate(format: "label == %@", label)).firstMatch
            if element.exists && !element.frame.isEmpty && app.frame.contains(element.frame) {
                let frame = element.frame
                app.coordinate(withNormalizedOffset: .zero)
                    .withOffset(CGVector(dx: frame.midX, dy: frame.midY)).tap()
                return
            }
            app.swipeUp()
        }
        XCTFail("Missing scrollable button: \(label)")
    }

    private func visibleTextBox(_ marker: String) throws -> CGRect? {
        // Scroll can leave AX frames stale/zero in Compose 1.7. Verify actual rendered pixels.
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.usesCPUOnly = true // Keep hosted simulator checks independent of GPU availability.
        request.recognitionLanguages = ["ko-KR", "en-US"]
        request.usesLanguageCorrection = false
        let image = try XCTUnwrap(app.screenshot().image.cgImage)
        try VNImageRequestHandler(cgImage: image, options: [:]).perform([request])
        let normalized = marker.replacingOccurrences(of: " ", with: "")
        return request.results?.first { item in
            item.topCandidates(1).first?.string.replacingOccurrences(of: " ", with: "").contains(normalized) == true
        }?.boundingBox
    }

    private func screenContains(_ marker: String) throws -> Bool {
        try visibleTextBox(marker) != nil
    }

    func testCloseReopenSelectAndRetainSelection() {
        app.launchArguments = ["--preview-dialog"]
        app.launch()
        XCTAssertEqual(app.state, .runningForeground, "Shared preview host must be in foreground")
        let initial = XCTAttachment(screenshot: app.screenshot())
        initial.name = "template-preview-first-launch"
        initial.lifetime = .keepAlways
        add(initial)
        tapButton("닫기")
        XCTAssertTrue(text("템플릿을 선택해 주세요.").waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons["이 템플릿 선택"].exists)
        tapButton("표 미리보기")
        tapButton("이 템플릿 선택")
        XCTAssertTrue(text("선택한 템플릿 · 현장 기록").waitForExistence(timeout: 10))
        tapButton("표 미리보기")
        tapButton("닫기")
        XCTAssertFalse(app.buttons["이 템플릿 선택"].exists)
        XCTAssertTrue(text("선택한 템플릿 · 현장 기록").waitForExistence(timeout: 10))
    }

    private func homeTemplateButton() -> XCUIElement {
        let element = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "템플릿 선택")).firstMatch
        XCTAssertTrue(element.waitForExistence(timeout: 20))
        XCTAssertFalse(element.frame.isEmpty)
        XCTAssertTrue(app.frame.contains(element.frame))
        return element
    }

    private func openHomeTemplates() {
        let frame = homeTemplateButton().frame
        app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: frame.midX, dy: frame.midY)).tap()
        XCTAssertTrue(text("표 템플릿").waitForExistence(timeout: 10))
    }

    func testHomeTemplatePreviewSelectionAndBack() throws {
        app.launchArguments = ["--navigation-flow", "--reset-navigation-fixture"]
        app.launch()
        XCTAssertTrue(homeTemplateButton().exists)
        XCTAssertTrue(try screenContains("기본 촬영"))
        XCTAssertTrue(try screenContains("다음 촬영 번호 0001"))
        // Other tests legitimately create PhotoKit assets in the same app sandbox.
        XCTAssertTrue(text("최근 촬영").waitForExistence(timeout: 10))
        let home = XCTAttachment(screenshot: app.screenshot())
        home.name = "shared-home-empty"
        home.lifetime = .keepAlways
        add(home)
        openHomeTemplates()
        let listScreen = XCTAttachment(screenshot: app.screenshot())
        listScreen.name = "shared-template-list"
        listScreen.lifetime = .keepAlways
        add(listScreen)
        XCTAssertTrue(button("+ 새 템플릿").isEnabled, "Existing catalog management is now connected")
        tapButton("표 확대 보기 · 현장 기록")
        tapButton("닫기")
        XCTAssertTrue(text("표 템플릿").waitForExistence(timeout: 10))
        tapButton("표 확대 보기 · 현장 기록")
        tapButton("이 템플릿 선택")
        XCTAssertTrue(homeTemplateButton().exists)
        XCTAssertTrue(try screenContains("현장 기록"))
        openHomeTemplates()
        XCTAssertTrue(app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS %@", "사용 중")).firstMatch
            .waitForExistence(timeout: 10))
        tapButton("뒤로가기")
        XCTAssertTrue(homeTemplateButton().exists)
        XCTAssertTrue(try screenContains("현장 기록"))
        XCTAssertTrue(try screenContains("다음 촬영 번호 0001"))
    }

    func testHomeSaveInfoDisplaysAndDismisses() throws {
        app.launchArguments = ["--navigation-flow", "--reset-navigation-fixture"]
        app.launch()
        tapButton("저장 정보")
        XCTAssertTrue(text("다음 촬영 저장 정보").waitForExistence(timeout: 10))
        XCTAssertTrue(text("템플릿을 선택해 주세요.").waitForExistence(timeout: 10))
        XCTAssertFalse(text("ios-sample_0001").exists)
        XCTAssertFalse(text("PoC/현장 기록/").exists)
        XCTAssertFalse(text("PoC/보관/").exists)
        XCTAssertFalse(button("저장 설정 열기").isEnabled, "Unavailable settings must stay disabled")
        let sheet = XCTAttachment(screenshot: app.screenshot())
        sheet.name = "shared-home-save-info"
        sheet.lifetime = .keepAlways
        add(sheet)
        // Tap the visible scrim above the bottom sheet, then verify actual dismissal.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15)).tap()
        XCTAssertTrue(homeTemplateButton().exists)
        XCTAssertFalse(text("다음 촬영 저장 정보").exists)
        tapButton("저장 정보")
        XCTAssertTrue(text("다음 촬영 저장 정보").waitForExistence(timeout: 10))
        XCTAssertTrue(text("템플릿을 선택해 주세요.").exists)
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15)).tap()

        openHomeTemplates()
        tapButton("표 확대 보기 · 현장 기록")
        tapButton("이 템플릿 선택")
        tapButton("저장 정보")
        tapButton("저장 설정 열기")
        XCTAssertTrue(text("표 편집").waitForExistence(timeout: 10))
        XCTAssertTrue(panelText("파일명").waitForExistence(timeout: 10),
                      "Android parity requires Home save settings to open the editor Save tab")
    }

    func testHomeTableEditorRequiresActiveTemplate() {
        app.launchArguments = ["--navigation-flow", "--reset-navigation-fixture"]
        app.launch()
        tapButton("표 편집")
        XCTAssertTrue(text("표 템플릿").waitForExistence(timeout: 10),
                      "Without an active template, Android routes Home editing to template selection")
        XCTAssertTrue(button("+ 새 템플릿").isEnabled)
        tapButton("뒤로가기")
        XCTAssertTrue(homeTemplateButton().exists)
    }

    private func panelText(_ label: String) -> XCUIElement {
        // Compose merges an icon/title/value into newline-separated AX labels.
        app.descendants(matching: .any).matching(NSPredicate(
            format: "label == %@ OR label BEGINSWITH %@ OR label ENDSWITH %@ OR label CONTAINS %@",
            label, label + "\n", "\n" + label, "\n" + label + "\n"
        )).firstMatch
    }

    private func tapPanelText(_ label: String) {
        let element = panelText(label)
        XCTAssertTrue(element.waitForExistence(timeout: 10), "Missing panel label: \(label)")
        let frame = element.frame
        XCTAssertFalse(frame.isEmpty)
        app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: frame.midX, dy: frame.midY)).tap()
    }

    private func tapScrollablePanelText(_ label: String, destination: XCUIElement? = nil) {
        for _ in 0..<5 {
            let element = panelText(label)
            if element.exists && element.isHittable && !element.frame.isEmpty && app.frame.contains(element.frame) {
                element.tap()
            } else if let box = try? visibleTextBox(label) {
                app.coordinate(withNormalizedOffset: CGVector(dx: box.midX, dy: 1 - box.midY)).tap()
            } else {
                app.swipeUp()
                continue
            }
            guard let destination = destination else { return }
            if destination.waitForExistence(timeout: 5) { return }
            // A tap during scrolling may only stop the scroll. Retry only on the source screen.
            guard text("설정").exists else { break }
        }
        XCTFail("Could not open settings row: \(label)\n\(app.debugDescription)")
    }

    private func typeWithVisibleKeyboard(_ value: String) {
        guard let firstCharacter = value.first else { return }
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 10))
        // Fresh simulators may show iOS's sliding-keyboard introduction over the keys.
        let introductionLabel = "Speed up your typing by sliding your finger across the letters to compose a word."
        let introduction = app.staticTexts[introductionLabel]
        if introduction.waitForExistence(timeout: 2) {
            let next = app.buttons["Continue"]
            XCTAssertTrue(next.exists)
            next.tap()
            // Native negative `exists` queries retry internally. A five-second
            // predicate waiter can time out while its final AX query already sees
            // dismissal. Observe fresh match counts AND the next usable key instead.
            let introductionMatches = app.staticTexts.matching(NSPredicate(
                format: "label == %@", introductionLabel
            ))
            func introductionDismissedAndKeyReady() -> Bool {
                guard introductionMatches.count == 0 else { return false }
                let firstKey = app.keyboards.firstMatch.keys[String(firstCharacter)]
                guard firstKey.exists else { return false }
                let frame = firstKey.frame
                return !frame.isEmpty && app.frame.contains(frame) && firstKey.isHittable
            }
            let deadline = Date().addingTimeInterval(15)
            var ready = introductionDismissedAndKeyReady()
            while !ready && Date() < deadline {
                ready = introductionDismissedAndKeyReady()
            }
            // Keep the completed final observation even if a slow AX query crosses
            // the deadline; do not replace its result with a waiter's timeout code.
            XCTAssertTrue(ready, "Keyboard introduction must disappear and the first input key must be usable")
            guard ready else { return }
        }
        // Compose 1.7 does not publish native keyboard focus to XCTest's typeText.
        // Real touches on visible lowercase keys go through the platform input connection.
        for character in value {
            let key = app.keyboards.firstMatch.keys[String(character)]
            XCTAssertTrue(key.waitForExistence(timeout: 5), "Missing visible keyboard key: \(character)")
            let frame = key.frame
            XCTAssertFalse(frame.isEmpty)
            app.coordinate(withNormalizedOffset: .zero)
                .withOffset(CGVector(dx: frame.midX, dy: frame.midY)).tap()
        }
    }

    func testSettingsQualityAccessibilityAndPersistence() {
        app.launchArguments = ["--navigation-flow", "--reset-navigation-fixture"]
        app.launch()
        tapButton("설정")
        let quality = panelText("사진 품질")
        XCTAssertTrue(quality.waitForExistence(timeout: 10),
                      "Photo quality row must expose its title, not only the decorative expand icon")
        XCTAssertFalse(quality.frame.isEmpty)
        tapPanelText("사진 품질")
        tapPanelText("화질 우선")
        XCTAssertTrue(panelText("화질 우선").waitForExistence(timeout: 10),
                      "Quality choice must appear in the collapsed settings row")
        app.terminate()
        app.launchArguments = ["--navigation-flow"]
        app.launch()
        tapButton("설정")
        XCTAssertTrue(panelText("화질 우선").waitForExistence(timeout: 10),
                      "Quality choice must survive an app relaunch")
    }

    func testCaptureHapticSettingPersistsFromGlobalSettings() throws {
        app.launchArguments = ["--navigation-flow", "--reset-navigation-fixture"]
        app.launch()
        tapButton("설정")
        tapPanelText("촬영 효과음 및 진동")
        XCTAssertTrue(text("촬영 진동: 켜짐").waitForExistence(timeout: 10))
        XCTAssertTrue(try screenContains("시스템 설정과 지역 정책"))
        tapPanelText("강함")
        tapPanelText("OFF")
        XCTAssertTrue(text("촬영 진동: 꺼짐").waitForExistence(timeout: 10))
        XCTAssertFalse(button("진동 미리보기").isEnabled)

        app.terminate()
        app.launchArguments = ["--navigation-flow"]
        app.launch()
        tapButton("설정")
        tapPanelText("촬영 효과음 및 진동")
        XCTAssertTrue(text("촬영 진동: 꺼짐").waitForExistence(timeout: 10))
    }

    func testPhotoPermissionDeniedThenGrantedInSettingsRestoresGallery() {
        app.resetAuthorizationStatus(for: .photos)
        var restored = false
        defer {
            if !restored {
                app.terminate()
                app.resetAuthorizationStatus(for: .photos)
            }
        }
        app.launchArguments = ["--camera-flow", "--deny-camera-permission-for-ui-test"]
        app.launch()
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 15))
        XCTAssertFalse(button("촬영").isEnabled)
        tapButton("앨범 보기")
        tapButton("사진 접근 허용하기")
        let system = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let deny = system.alerts.buttons.matching(NSPredicate(
            format: "label IN %@", ["Don’t Allow", "Don't Allow"]
        )).firstMatch
        XCTAssertTrue(deny.waitForExistence(timeout: 10))
        deny.tap()
        XCTAssertTrue(text("거부됨").waitForExistence(timeout: 10))
        XCTAssertTrue(panelText("DZlog 설정에서 사진 접근을 허용해 주세요. 앱 목록이 열리면 DZlog를 선택하세요.").exists)
        tapButton("iPhone 설정 열기")
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        XCTAssertTrue(settings.wait(for: .runningForeground, timeout: 10))
        // iOS can open either this app's settings or Settings > Apps. The
        // latter also lists the UI test runner, so match the app's bundle ID.
        let photos = settings.staticTexts["Photos"].firstMatch
        let appRow = settings.buttons["com.dudoziworkshop.dzlog"]
        let appsList = settings.navigationBars["Apps"]
        let reachedSettings = NSPredicate { _, _ in
            if appsList.exists { return appRow.exists && appRow.isHittable }
            return photos.exists && photos.isHittable
        }
        let route = XCTNSPredicateExpectation(predicate: reachedSettings, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [route], timeout: 10), .completed, settings.debugDescription)
        if appsList.exists {
            appRow.tap()
            let leftList = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: appsList)
            XCTAssertEqual(XCTWaiter.wait(for: [leftList], timeout: 10), .completed, settings.debugDescription)
        }
        XCTAssertTrue(photos.waitForExistence(timeout: 10), settings.debugDescription)
        photos.tap()
        let fullAccess = settings.descendants(matching: .any).matching(NSPredicate(
            format: "label IN %@", ["Full Access", "All Photos"]
        )).firstMatch
        XCTAssertTrue(fullAccess.waitForExistence(timeout: 10), settings.debugDescription)
        XCTAssertTrue(fullAccess.isHittable, settings.debugDescription)
        fullAccess.tap()
        // A confirmation can be delivered after the Settings row tap, and
        // iOS can host it in either Settings or SpringBoard.
        let settingsConfirm = settings.alerts.buttons["Allow Full Access"].firstMatch
        let systemConfirm = system.alerts.buttons["Allow Full Access"].firstMatch
        let grantReady = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            settingsConfirm.exists || systemConfirm.exists || (fullAccess.exists && fullAccess.isSelected)
        }, object: nil)
        _ = XCTWaiter.wait(for: [grantReady], timeout: 6)
        if settingsConfirm.exists {
            settingsConfirm.tap()
        } else if systemConfirm.exists {
            systemConfirm.tap()
        }
        let confirmationClosed = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            !settingsConfirm.exists && !systemConfirm.exists
        }, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [confirmationClosed], timeout: 10), .completed, settings.debugDescription)
        // The gallery assertion below is the end-to-end check. Record the
        // system selection too so a failed grant is not mistaken for stale UI.
        let settingsAfterGrant = settings.debugDescription
        // iOS may terminate the app when its privacy settings change.
        if app.state == .notRunning { app.launch() } else { app.activate() }
        let gallery = app.buttons["폴더 보기"]
        let album = app.buttons["앨범 보기"]
        let returnedToApp = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            gallery.exists || (album.exists && album.isHittable)
        }, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [returnedToApp], timeout: 15), .completed, app.debugDescription)
        var openedFromHome = false
        if !gallery.exists {
            openedFromHome = app.buttons["촬영 시작"].exists
            tapButton("앨범 보기")
        }
        XCTAssertTrue(app.buttons["폴더 보기"].waitForExistence(timeout: 15),
                      "After Settings grant:\n\(settingsAfterGrant)\nAfter app activation:\n\(String(describing: app.debugDescription))")
        XCTAssertFalse(text("거부됨").exists)
        XCTAssertFalse(app.buttons["선택한 사진 변경"].exists, "Full Access must not restore as Limited")
        restored = true
        tapButton("뒤로가기")
        if openedFromHome {
            XCTAssertTrue(app.buttons["촬영 시작"].waitForExistence(timeout: 10), app.debugDescription)
            XCTAssertTrue(album.exists)
        } else {
            XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 10), app.debugDescription)
            XCTAssertFalse(button("촬영").isEnabled)
        }
    }

    func testPermissionsSettingsExposeCurrentStateAndSystemSettingsRoute() throws {
        app.launchArguments = ["--navigation-flow", "--reset-navigation-fixture"]
        app.launch()
        tapButton("설정")
        tapPanelText("권한 관리")
        XCTAssertTrue(text("카메라 권한").waitForExistence(timeout: 10))
        XCTAssertTrue(text("사진 권한").exists)
        XCTAssertTrue(panelText("촬영 결과는 iPhone 사진 보관함에 저장돼요. DZlog 안에서는 프로젝트와 폴더별로 확인할 수 있어요.").exists)
        XCTAssertTrue(button("상태 새로고침").isEnabled)
        XCTAssertTrue(button("iPhone 설정 열기").isEnabled)
        tapButton("상태 새로고침")
        tapButton("iPhone 설정 열기")
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        XCTAssertTrue(settings.wait(for: .runningForeground, timeout: 10))
        app.activate()
        XCTAssertTrue(text("권한 관리").waitForExistence(timeout: 10))
        XCTAssertTrue(text("카메라 권한").exists)
    }

    func testHelpAndCreditsSettingsRoutesAreReadableAndSafe() throws {
        app.launchArguments = ["--navigation-flow", "--reset-navigation-fixture"]
        app.launch()
        tapButton("설정")
        XCTAssertTrue(text("버전 1.0.0 (3)").waitForExistence(timeout: 10))
        tapScrollablePanelText("사용법 및 도움말")
        XCTAssertTrue(text("DZlog의 주요 기능을 순서대로 살펴볼 수 있어요.").waitForExistence(timeout: 10))
        tapPanelText("카메라 촬영")
        XCTAssertTrue(text("촬영 화면 구성").waitForExistence(timeout: 10))
        XCTAssertTrue(text("단계 1/4").exists)
        XCTAssertTrue(try screenContains("읽기 전용 안내입니다"))
        tapButton("다음")
        XCTAssertTrue(text("빠른 수정").waitForExistence(timeout: 10))
        tapButton("뒤로가기")
        tapPanelText("처음 사용법 다시 보기")
        XCTAssertTrue(text("1 / 3").waitForExistence(timeout: 10))
        XCTAssertTrue(try screenContains("찍고, 기록하고"))
        tapButton("뒤로가기")
        XCTAssertTrue(text("DZlog의 주요 기능을 순서대로 살펴볼 수 있어요.").waitForExistence(timeout: 10))
        tapButton("뒤로가기")
        XCTAssertTrue(text("설정").waitForExistence(timeout: 10))
        tapScrollablePanelText("도움 주신 분들", destination: text("함께해 주셔서 감사합니다"))
        XCTAssertTrue(text("버전 1.0.0 (3)").exists)
        XCTAssertTrue(text("특별한 감사").exists)
        XCTAssertTrue(text("루루").exists)
        XCTAssertTrue(text("채채").exists)
        XCTAssertTrue(text("작은박").exists)
    }

    func testWorkspaceExportImportPreviewsAndAddsWithoutReplacing() throws {
        app.launchArguments = [
            "--navigation-flow", "--reset-navigation-fixture", "--workspace-backup-ui-test"
        ]
        app.launch()
        tapButton("설정")
        tapScrollablePanelText("작업 환경 내보내기")
        XCTAssertTrue(text("파일에 포함돼요").waitForExistence(timeout: 10))
        XCTAssertTrue(try screenContains("현재 자동번호"))
        tapButton("백업 파일 내보내기")
        XCTAssertTrue(text("백업 파일을 내보냈습니다.").waitForExistence(timeout: 10))
        tapButton("뒤로가기")
        tapScrollablePanelText("작업 환경 가져오기")
        XCTAssertTrue(text("기존 템플릿은 유지돼요").waitForExistence(timeout: 10))
        tapButton("백업 파일 선택")
        XCTAssertTrue(text("선택한 DZlog 백업").waitForExistence(timeout: 10))
        XCTAssertTrue(text("1개 유지").exists)
        XCTAssertTrue(text("1개 추가").exists)
        XCTAssertTrue(text("1개 자동 구분").exists)
        tapButton("가져오기")
        XCTAssertTrue(text("1개 템플릿을 추가했습니다.").waitForExistence(timeout: 10))
        tapButton("뒤로가기")
        tapButton("뒤로가기")
        openHomeTemplates()
        XCTAssertTrue(app.buttons["표 확대 보기 · 현장 기록"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["표 확대 보기 · 현장 기록 (가져옴)"].exists)
    }

    func testHomeCatalogSettingsAndRelaunch() throws {
        app.launchArguments = ["--navigation-flow", "--reset-navigation-fixture"]
        app.launch()
        tapButton("설정")
        tapPanelText("사진 품질")
        tapPanelText("화질 우선")
        XCTAssertTrue(panelText("화질 우선").exists)
        tapButton("뒤로가기")
        openHomeTemplates()
        tapButton("템플릿 메뉴")
        tapPanelText("복제")
        XCTAssertTrue(app.buttons["표 확대 보기 · 현장 기록 사본"].waitForExistence(timeout: 10))
        tapButton("템플릿 메뉴")
        tapPanelText("이름 변경")
        XCTAssertTrue(text("템플릿 이름 변경").waitForExistence(timeout: 10))
        // The existing rename field contains the current template name.
        let nameField = try XCTUnwrap(app.buttons.matching(NSPredicate(
            format: "label == %@", "현장 기록 사본"
        )).allElementsBoundByIndex.first)
        let nameFrame = nameField.frame
        app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: nameFrame.minX + nameFrame.width * 0.25, dy: nameFrame.midY)).tap()
        typeWithVisibleKeyboard("navqa")
        tapButton("저장")
        let renamed = app.buttons.matching(NSPredicate(
            format: "label BEGINSWITH %@ AND label CONTAINS %@", "표 확대 보기 · ", "navqa"
        )).firstMatch
        XCTAssertTrue(renamed.waitForExistence(timeout: 10))
        let renamedLabel = renamed.label
        tapButton("템플릿 메뉴")
        tapPanelText("삭제")
        XCTAssertTrue(text("템플릿을 삭제할까요?").waitForExistence(timeout: 10))
        tapButton("삭제")
        XCTAssertFalse(app.buttons[renamedLabel].exists)
        tapButton("+ 새 템플릿")
        XCTAssertTrue(text("표 편집").waitForExistence(timeout: 10))
        tapButton("저장")
        if app.buttons["셀 편집 닫기"].exists { tapButton("셀 편집 닫기") }
        tapButton("뒤로가기")
        XCTAssertTrue(text("표 템플릿").waitForExistence(timeout: 10))
        tapButton("표 확대 보기 · 새 템플릿")
        tapButton("이 템플릿 선택")
        XCTAssertTrue(homeTemplateButton().exists)
        app.terminate()
        app.launchArguments = ["--navigation-flow"]
        app.launch()
        tapButton("설정")
        XCTAssertTrue(panelText("화질 우선").waitForExistence(timeout: 10), "Settings must survive relaunch")
        tapButton("뒤로가기")
        openHomeTemplates()
        XCTAssertTrue(app.buttons["표 확대 보기 · 새 템플릿"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["표 확대 보기 · 현장 기록"].exists, "Original template must remain")
        XCTAssertFalse(app.buttons[renamedLabel].exists, "Deleted copy must remain deleted")
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = "shared-catalog-restored"
        image.lifetime = .keepAlways
        add(image)
    }

    func testProductionOnboardingSelectsStarterAndPersistsWorkspace() throws {
        app.launchArguments = ["--production-home-flow", "--reset-production-home-fixture"]
        app.launch()

        XCTAssertTrue(try screenContains("찍고, 기록하고"))
        tapButton("시작하기")
        XCTAssertTrue(try screenContains("사진 한 장으로"))
        tapButton("다음")
        XCTAssertTrue(text("어떤 기록을 시작할까요?").waitForExistence(timeout: 10))
        XCTAssertTrue(text("기본 촬영").waitForExistence(timeout: 10))
        tapButton("이 양식으로 시작")
        XCTAssertTrue(homeTemplateButton().waitForExistence(timeout: 10))
        XCTAssertTrue(try screenContains("기본 촬영"))
        XCTAssertFalse(text("현장 기록").exists)
        XCTAssertFalse(text("블루베리 관찰 기록").exists)
        tapButton("설정")
        tapPanelText("사진 품질")
        tapPanelText("화질 우선")

        app.terminate()
        app.launchArguments = ["--production-home-flow"]
        app.launch()
        XCTAssertFalse(try screenContains("찍고, 기록하고"),
                       "Completed onboarding must not reopen after relaunch")
        tapButton("설정")
        XCTAssertTrue(panelText("화질 우선").waitForExistence(timeout: 10))
        tapButton("뒤로가기")
        tapButton("표 편집")
        XCTAssertTrue(text("표 편집").waitForExistence(timeout: 10),
                      "The selected production starter must reopen directly after relaunch")
    }

    func testProductionProjectCreateSelectAndRelaunch() throws {
        app.launchArguments = ["--production-home-flow", "--reset-production-home-fixture",
                               "--deny-camera-permission-for-ui-test"]
        app.launch()
        enterHomeAfterOptionalOnboarding()

        tapButton("촬영 프로젝트 선택 · 일반 촬영")
        XCTAssertTrue(text("촬영 프로젝트 선택").waitForExistence(timeout: 10))
        tapButton("+ 새 프로젝트")
        XCTAssertTrue(text("프로젝트").waitForExistence(timeout: 10))
        XCTAssertTrue(text("새 프로젝트").waitForExistence(timeout: 10))
        tapPanelText("프로젝트 이름")
        typeWithVisibleKeyboard("blueberry")
        tapButton("만들기")
        XCTAssertTrue(panelText("프로젝트 상세 · blueberry").waitForExistence(timeout: 10))
        tapPanelText("프로젝트 상세 · blueberry")
        XCTAssertTrue(text("프로젝트 상세").waitForExistence(timeout: 10))
        tapButton("프로젝트 관리")
        tapPanelText("프로젝트 및 연결 사진 삭제")
        XCTAssertTrue(text("결과 사진 0장 · 원본 사진 0장").waitForExistence(timeout: 10))
        tapButton("취소")
        XCTAssertTrue(text("프로젝트 상세").waitForExistence(timeout: 10))
        tapButton("프로젝트 목록")
        tapButton("뒤로")

        tapButton("촬영 프로젝트 선택 · 일반 촬영")
        tapButton("blueberry")
        XCTAssertTrue(text("촬영 프로젝트 선택 · blueberry").waitForExistence(timeout: 10), app.debugDescription)

        tapButton("촬영 시작")
        // The first production capture requires a saved placement and counter.
        if app.buttons["확인"].waitForExistence(timeout: 5) {
            tapButton("확인")
            tapCaptureSettingsButton("저장", searchDown: false)
            tapButton("촬영 시작")
        }
        XCTAssertTrue(button("카메라 프로젝트 변경").waitForExistence(timeout: 20))
        let info = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "촬영 정보 열기")).firstMatch
        XCTAssertTrue(info.waitForExistence(timeout: 10))
        info.tap()
        XCTAssertTrue(text("촬영 정보").waitForExistence(timeout: 10))
        let template = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "템플릿 선택 ·")).firstMatch
        XCTAssertTrue(template.waitForExistence(timeout: 10))
        template.tap()
        XCTAssertTrue(text("표 템플릿").waitForExistence(timeout: 10))
        tapButton("뒤로가기")
        XCTAssertTrue(info.waitForExistence(timeout: 10))
        for action in ["표 상세설정", "저장 설정"] {
            info.tap()
            tapButton(action)
            XCTAssertTrue(text("표 편집").waitForExistence(timeout: 10))
            if action == "저장 설정" {
                XCTAssertTrue(panelText("파일명").waitForExistence(timeout: 10))
            }
            // Back closes the current editor panel before returning to the camera.
            for _ in 0..<3 {
                if info.exists { break }
                tapButton("뒤로가기")
                if info.waitForExistence(timeout: 2) { break }
            }
            XCTAssertTrue(info.exists)
        }
        info.tap()
        tapButton("프로젝트 선택 · blueberry")
        XCTAssertTrue(text("촬영 프로젝트 선택").waitForExistence(timeout: 10))
        XCTAssertTrue(text("blueberry").exists)
        tapButton("닫기")
        tapButton("촬영 결과 보기")
        XCTAssertTrue(text("프로젝트 결과").waitForExistence(timeout: 10))
        XCTAssertTrue(text("확인할 수 있는 촬영 사진이 없습니다.").waitForExistence(timeout: 10))
        tapButton("뒤로")
        tapButton("뒤로가기")
        XCTAssertTrue(button("촬영 시작").waitForExistence(timeout: 20))

        app.terminate()
        app.launchArguments = ["--production-home-flow"]
        app.launch()
        XCTAssertTrue(text("촬영 프로젝트 선택 · blueberry").waitForExistence(timeout: 20),
                      "Selected project must survive relaunch")
        tapButton("촬영 프로젝트 선택 · blueberry")
        tapButton("일반 촬영")
        XCTAssertTrue(text("촬영 프로젝트 선택 · 일반 촬영").waitForExistence(timeout: 10))
        app.terminate()
        app.launch()
        XCTAssertTrue(text("촬영 프로젝트 선택 · 일반 촬영").waitForExistence(timeout: 20))
        tapButton("촬영 프로젝트 선택 · 일반 촬영")
        tapButton("blueberry")
        XCTAssertTrue(text("촬영 프로젝트 선택 · blueberry").waitForExistence(timeout: 10), app.debugDescription)
    }

    func testEditorContentStructureStyleSaveAndRelaunch() throws {
        app.launchArguments = ["--editor-flow", "--reset-editor-fixture"]
        app.launch()
        openHomeTemplates()
        tapButton("표 확대 보기 · 현장 기록")
        tapButton("이 템플릿 선택")
        tapButton("표 편집")
        XCTAssertTrue(text("표 편집").waitForExistence(timeout: 15))
        XCTAssertTrue(app.buttons["셀 편집 닫기"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.keyboards.firstMatch.exists)
        // The first cell opens its panel on entry. Compose 1.7 exposes the unfocused
        // field as a Button even while focused; touch the lower panel field first.
        let valueField = try XCTUnwrap(app.buttons.matching(NSPredicate(
            format: "label == %@", "블루베리 관찰 기록"
        )).allElementsBoundByIndex.max { $0.frame.midY < $1.frame.midY })
        let valueFrame = valueField.frame
        XCTAssertFalse(valueFrame.isEmpty)
        app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: valueFrame.minX + valueFrame.width * 0.25, dy: valueFrame.midY)).tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 10), "Cell input must open the keyboard")
        typeWithVisibleKeyboard("iosqa")
        XCTAssertTrue(try screenContains("iosqa"), "Typed text must appear in the existing editor")
        // Shared back policy closes the keyboard before changing tabs.
        tapButton("뒤로가기")
        let editedCell = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "iosqa")).firstMatch
        XCTAssertTrue(editedCell.waitForExistence(timeout: 10),
                      "Edited contents must remain in the table after keyboard dismissal")
        let savedCellText = editedCell.label
        XCTAssertTrue(app.frame.contains(editedCell.frame))
        tapPanelText("구조")
        XCTAssertFalse(text("3행과 4행 높이 조절").exists)
        tapButton("+\n행 추가")
        XCTAssertTrue(text("3행과 4행 높이 조절").waitForExistence(timeout: 10))
        tapButton("실행 취소")
        XCTAssertFalse(text("3행과 4행 높이 조절").exists)
        tapButton("다시 실행")
        XCTAssertTrue(text("3행과 4행 높이 조절").exists)
        tapPanelText("스타일")
        tapPanelText("투명")
        XCTAssertFalse(text("배경 투명도").exists)
        tapPanelText("저장설정")
        XCTAssertTrue(panelText("파일명").waitForExistence(timeout: 10))
        tapPanelText("파일명")
        tapButton("+ 구성요소 추가")
        tapButton("직접입력")
        tapPanelText("값 없음")
        XCTAssertTrue(text("직접 입력").waitForExistence(timeout: 10))
        // An empty Compose field has no AX label in 1.7. Locate the unique visible
        // input between this sheet's heading and preview, using its actual bounds.
        let heading = text("직접 입력").frame
        let previewHeading = text("미리보기").frame
        XCTAssertFalse(heading.isEmpty)
        XCTAssertFalse(previewHeading.isEmpty)
        let filenameFields = app.buttons.matching(NSPredicate(format: "label == %@", ""))
            .allElementsBoundByIndex.filter {
                let frame = $0.frame
                return !frame.isEmpty && frame.minY >= heading.maxY
                    && frame.maxY <= previewHeading.minY && app.frame.contains(frame)
            }
        XCTAssertEqual(filenameFields.count, 1, "Expected one empty input in the manual filename sheet")
        let filenameFrame = try XCTUnwrap(filenameFields.first).frame
        app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: filenameFrame.minX + filenameFrame.width * 0.25, dy: filenameFrame.midY)).tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 10))
        typeWithVisibleKeyboard("iosreview")
        tapButton("확인")
        XCTAssertTrue(try screenContains("iosreview"))
        for _ in 0..<3 {
            if !text("파일명 설정").exists { break }
            tapButton("뒤로가기")
        }
        XCTAssertFalse(text("파일명 설정").exists)
        let review = XCTAttachment(screenshot: app.screenshot())
        review.name = "shared-editor-save-tab"
        review.lifetime = .keepAlways
        add(review)
        tapPanelText("내용")
        tapButton("저장")
        // With no selected cell/panel and a successful save, Back returns directly to Home.
        for _ in 0..<3 {
            if app.buttons["템플릿 선택"].exists { break }
            tapButton("뒤로가기")
        }
        XCTAssertTrue(homeTemplateButton().exists)
        app.terminate()
        app.launchArguments = ["--editor-flow"]
        app.launch()
        tapButton("표 편집")
        // Exact rendered cell semantics avoid OCR confusing q/g in mixed Korean/Latin text.
        let restoredCell = app.buttons.matching(NSPredicate(format: "label == %@", savedCellText)).firstMatch
        XCTAssertTrue(restoredCell.waitForExistence(timeout: 10), "Exact saved cell text must survive app relaunch")
        XCTAssertFalse(restoredCell.frame.isEmpty)
        XCTAssertTrue(app.frame.contains(restoredCell.frame))
        let restoredReview = XCTAttachment(screenshot: app.screenshot())
        restoredReview.name = "shared-editor-restored-contents"
        restoredReview.lifetime = .keepAlways
        add(restoredReview)
        tapPanelText("구조")
        XCTAssertTrue(restoredCell.exists, "Structural change must retain exact edited contents")
        XCTAssertTrue(app.frame.contains(restoredCell.frame))
        XCTAssertTrue(text("3행과 4행 높이 조절").exists, "Added row must survive relaunch")
        tapPanelText("스타일")
        XCTAssertFalse(text("배경 투명도").exists, "Saved transparent style must survive relaunch")
        tapPanelText("저장설정")
        XCTAssertTrue(try screenContains("iosreview"), "Saved naming rule must survive relaunch")
    }

    func testHorizontalGestureRevealsLastColumn() throws {
        app.launchArguments = ["--large-table"]
        app.launch()
        XCTAssertTrue(text("왼쪽 시작").waitForExistence(timeout: 20))
        XCTAssertFalse(try screenContains("오른쪽 끝"), "Fixture must overflow horizontally")
        let y = text("왼쪽 시작").frame.midY
        for _ in 0..<10 {
            if try screenContains("오른쪽 끝") { break }
            app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: app.frame.width * 0.78, dy: y))
                .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: .zero)
                    .withOffset(CGVector(dx: app.frame.width * 0.22, dy: y)))
        }
        XCTAssertTrue(try screenContains("오른쪽 끝"), "Horizontal gesture did not reveal last column")
        tapButton("닫기")
        XCTAssertFalse(app.buttons["이 템플릿 선택"].exists)
        XCTAssertTrue(text("템플릿을 선택해 주세요.").waitForExistence(timeout: 10))
    }

    func testVerticalGestureRevealsLastRow() throws {
        app.launchArguments = ["--large-table"]
        app.launch()
        XCTAssertTrue(text("왼쪽 시작").waitForExistence(timeout: 20))
        XCTAssertFalse(try screenContains("마지막 행"), "Fixture must overflow vertically")
        let top = text("왼쪽 시작").frame.midY
        for _ in 0..<12 {
            if try screenContains("마지막 행") { break }
            app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: app.frame.width * 0.4, dy: top + 240))
                .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: .zero)
                    .withOffset(CGVector(dx: app.frame.width * 0.4, dy: top)))
        }
        XCTAssertTrue(try screenContains("마지막 행"), "Vertical gesture did not reveal last row")
        tapButton("이 템플릿 선택")
        XCTAssertTrue(text("선택한 템플릿 · 스크롤 검증").waitForExistence(timeout: 10))
    }
}
