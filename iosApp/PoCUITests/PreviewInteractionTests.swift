import XCTest
import Vision

final class PreviewInteractionTests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
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
        let hierarchy = XCTAttachment(string: app.debugDescription)
        print("UI_HIERARCHY: \(app.debugDescription)")
        hierarchy.lifetime = .keepAlways
        add(hierarchy)
        app.terminate()
    }

    // Only the sensor JPEG is synthetic. Exercise the user settings form and
    // production configured save entry, then relaunch and read the real gallery.
    // These quick-value checks are the focused gate; milestone validation still
    // runs this class as part of the complete, unfiltered UI suite.
    func testQuickValuesCancelApplySaveAndRelaunch() {
        app.launchArguments = ["--quick-values-flow", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("빠른 입력 열기")
        tapPanelText("2행 1열 · 텍스트")
        typeWithVisibleKeyboard("blue")
        tapButton("입력 취소")
        XCTAssertTrue(text("상태: 대기").waitForExistence(timeout: 20))
        XCTAssertFalse(text("입력값: blue").exists)
        tapButton("빠른 입력 열기")
        tapPanelText("2행 1열 · 텍스트")
        typeWithVisibleKeyboard("blue")
        tapButton("입력 완료")
        XCTAssertTrue(text("입력값: blue").waitForExistence(timeout: 20))
        XCTAssertTrue(text("숫자값: 1.20").exists)
        XCTAssertTrue(text("다음 번호: 12").exists)
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(
            format: "label BEGINSWITH %@ AND label CONTAINS %@", "파일명: ", "blue")).firstMatch.exists)
        tapButton("빠른 입력 열기")
        tapScrollableButton("날짜 형식 · yyyyMMdd")
        tapScrollableButton("yyMMdd · 짧은 연월일")
        tapScrollableButton("문구 세트 · 과수원")
        tapScrollableButton("온실")
        tapButton("입력 완료")
        XCTAssertTrue(text("날짜형식: yyMMdd").waitForExistence(timeout: 20))
        XCTAssertTrue(text("문구세트: set-b").exists)
        tapButton("입력값으로 촬영 저장")
        XCTAssertTrue(waitForPhotoState("상태: 입력 반영 사진 저장 완료"))
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

    func testQuickValuesEligibilityNumbersAndStaleWorkspace() {
        app.launchArguments = ["--quick-values-flow", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("입력 정책 검증")
        XCTAssertTrue(text("상태: 편집 대상·숫자·충돌 차단 검증 완료").waitForExistence(timeout: 20), app.debugDescription)
        XCTAssertTrue(text("숫자값: 1.20").exists)
        XCTAssertTrue(text("다음 번호: 12").exists)
    }

    func testQuickValueTemplateSwitchPersistsDraftAndSelection() {
        app.launchArguments = ["--quick-values-flow", "--reset-quick-values-fixture"]
        app.launch()
        tapButton("빠른 입력 열기")
        tapPanelText("2행 1열 · 텍스트")
        typeWithVisibleKeyboard("blue")
        tapButton("템플릿 · 빠른 입력 검증")
        tapButton("빠른 입력 보조")
        tapButton("입력 완료")
        XCTAssertTrue(text("활성템플릿: 빠른 입력 보조").waitForExistence(timeout: 20))
        XCTAssertTrue(text("입력값: SECOND").exists)
        XCTAssertTrue(text("다음 번호: 40").exists)
        app.terminate()
        app.launchArguments = ["--quick-values-flow"]
        app.launch()
        XCTAssertTrue(text("활성템플릿: 빠른 입력 보조").waitForExistence(timeout: 20))
        tapButton("빠른 입력 열기")
        tapButton("템플릿 · 빠른 입력 보조")
        tapButton("빠른 입력 검증")
        tapButton("입력 완료")
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

    private func openLiveTable(reset: Bool) {
        app.launchArguments = ["--live-table-placement-flow"] + (reset ? ["--reset-live-table-fixture"] : [])
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

    func testNativeLiveTableMoveResizePersistsAndSaves() {
        openLiveTable(reset: true)
        let initialMove = String(describing: liveHandle("ios.table.move").value ?? "")
        dragLiveHandle("ios.table.move", dx: 55, dy: 45)
        let moved = String(describing: liveHandle("ios.table.move").value ?? "")
        XCTAssertNotEqual(moved, initialMove)
        let beforeResize = liveHandle("ios.table.move").frame
        let initialSize = String(describing: liveHandle("ios.table.resize").value ?? "")
        dragLiveHandle("ios.table.resize", dx: -35, dy: -20)
        let resized = String(describing: liveHandle("ios.table.resize").value ?? "")
        let resizedPosition = String(describing: liveHandle("ios.table.move").value ?? "")
        let afterResize = liveHandle("ios.table.move").frame
        XCTAssertNotEqual(resized, initialSize)
        XCTAssertLessThan(afterResize.width, beforeResize.width)
        XCTAssertLessThan(afterResize.height, beforeResize.height)
        // Offsets are percentages of the remaining free space. Resizing preserves
        // the displayed top-left and recomputes those percentages, rounded to 1%.
        let roundingTolerance = max(app.frame.width, app.frame.height) / 200 + 1
        XCTAssertEqual(afterResize.minX, beforeResize.minX, accuracy: roundingTolerance)
        XCTAssertEqual(afterResize.minY, beforeResize.minY, accuracy: roundingTolerance)
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

    func testConfiguredSaveModesAndGalleryAfterRelaunch() { verifyConfiguredModes(merged: false) }

    func testMergedConfiguredSaveModesAndGalleryAfterRelaunch() { verifyConfiguredModes(merged: true) }

    private func verifyConfiguredModes(merged: Bool) {
        for (label, mode, placement) in [("원본만", "ORIGINAL_ONLY", "없음"),
                                         ("결과만", "WATERMARK_ONLY", "BOTTOM_RIGHT"),
                                         ("원본과 결과", "BOTH", "BOTTOM_RIGHT")] {
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
        for label in ["원본만", "결과만", "원본과 결과"] {
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
        tapButton("원본만")
        tapButton("설정 저장")
        tapButton("카메라 열기")
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        tapButton("빠른 입력")
        XCTAssertTrue(panelText("2행 2열 · 텍스트").waitForExistence(timeout: 20))
        tapButton("입력 취소")
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        XCTAssertFalse(button("촬영").isEnabled)
        tapButton("촬영 설정")
        tapButton("원본과 결과")
        tapButton("설정 저장")
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        tapButton("뒤로가기")
        XCTAssertTrue(text("모드: BOTH").waitForExistence(timeout: 20))
        XCTAssertTrue(text("배치: BOTTOM_RIGHT").exists)
        XCTAssertTrue(text("다음 번호: 12").exists)
        app.terminate()
        app.launchArguments = configuredArguments(merged: true)
        app.launch()
        XCTAssertTrue(text("모드: BOTH").waitForExistence(timeout: 20))
        XCTAssertTrue(text("배치: BOTTOM_RIGHT").exists)
    }

    func testGalleryFolderGridViewerAndOriginalResultSwitch() {
        configureCaptureFixture("원본과 결과", merged: true)
        tapButton("촬영 저장")
        XCTAssertTrue(waitForPhotoState("상태: 설정대로 저장 완료"))
        tapButton("갤러리 열기")
        // Compose can merge the image description with the role and filename
        // on the clickable grid cell. The placeholder label is only "사진".
        let thumbnail = app.descendants(matching: .any).matching(NSPredicate(
            format: "label == %@ OR label BEGINSWITH %@", "사진 썸네일", "사진 썸네일\n"
        )).firstMatch
        XCTAssertTrue(thumbnail.waitForExistence(timeout: 30), app.debugDescription)
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
        tapButton("원본 보기")
        XCTAssertTrue(button("결과 보기").waitForExistence(timeout: 20))
        tapButton("결과 보기")
        tapButton("사진 목록")
        tapButton("폴더 보기")
        XCTAssertTrue(text("폴더: Pictures/DZlog/").waitForExistence(timeout: 20))
        tapButton("목록 보기")
        XCTAssertTrue(button("그리드 보기").exists)
        tapButton("전체 사진")
        tapButton("뒤로가기")
        XCTAssertTrue(text("다음 번호: 13").waitForExistence(timeout: 20))
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
        tapButton(modeLabel)
        tapButton("설정 저장")
        XCTAssertTrue(text("상태: 설정 복원 완료").waitForExistence(timeout: 20))
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
        XCTAssertTrue(text("카메라").waitForExistence(timeout: 20))
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        XCTAssertTrue(text("이번 촬영: 0장").exists)
        XCTAssertFalse(button("촬영").isEnabled)
    }

    // Real home entry uses the Swift permission gate and preserves the home VC on return.
    func testHomeCameraPermissionDeniedEntryAndReturn() {
        app.launchArguments = ["--home-navigation", "--deny-camera-permission-for-ui-test"]
        app.launch()
        XCTAssertTrue(button("촬영 시작").isEnabled)
        tapButton("촬영 시작")
        XCTAssertTrue(text("카메라").waitForExistence(timeout: 20))
        XCTAssertTrue(text("카메라 권한이 없습니다.").waitForExistence(timeout: 20))
        XCTAssertFalse(button("촬영").isEnabled)
        tapButton("뒤로가기")
        XCTAssertTrue(button("촬영 시작").waitForExistence(timeout: 20))
        XCTAssertTrue(text("최근 촬영").exists) // Home content is restored; counter text is not an exposed AX node.
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

    private func screenContains(_ marker: String) throws -> Bool {
        // Scroll can leave AX frames stale/zero in Compose 1.7. Verify actual rendered pixels.
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.usesCPUOnly = true // Keep hosted simulator checks independent of GPU availability.
        request.recognitionLanguages = ["ko-KR", "en-US"]
        request.usesLanguageCorrection = false
        let image = try XCTUnwrap(app.screenshot().image.cgImage)
        try VNImageRequestHandler(cgImage: image, options: [:]).perform([request])
        let lines = (request.results ?? []).compactMap { $0.topCandidates(1).first?.string }
        print("SCREEN_TEXT: \(lines)")
        let normalized = marker.replacingOccurrences(of: " ", with: "")
        return lines.contains { $0.replacingOccurrences(of: " ", with: "").contains(normalized) }
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

    private func typeWithVisibleKeyboard(_ value: String) {
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 10))
        // Fresh simulators may show iOS's sliding-keyboard introduction over the keys.
        let introduction = app.staticTexts["Speed up your typing by sliding your finger across the letters to compose a word."]
        if introduction.waitForExistence(timeout: 2) {
            let next = app.buttons["Continue"]
            XCTAssertTrue(next.exists)
            next.tap()
            let dismissed = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: introduction)
            XCTAssertEqual(XCTWaiter.wait(for: [dismissed], timeout: 5), .completed)
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

    func testEditorContentStructureStyleSaveAndRelaunch() throws {
        app.launchArguments = ["--editor-flow", "--reset-editor-fixture"]
        app.launch()
        tapButton("표 편집")
        XCTAssertTrue(text("표 편집").waitForExistence(timeout: 15))
        tapPanelText("블루베리 관찰 기록")
        XCTAssertTrue(app.buttons["셀 편집 닫기"].waitForExistence(timeout: 10))
        // Selecting the canvas cell opens its panel. Compose 1.7 exposes the unfocused
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

