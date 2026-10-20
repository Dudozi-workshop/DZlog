import XCTest
import Vision

final class PreviewInteractionTests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
    }

    override func tearDown() {
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

    private func text(_ label: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    private func button(_ label: String) -> XCUIElement {
        let element = app.buttons[label]
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
        app.launchArguments = ["--home-navigation"]
        app.launch()
        XCTAssertTrue(homeTemplateButton().exists)
        XCTAssertTrue(try screenContains("기본 촬영"))
        XCTAssertTrue(try screenContains("다음 촬영 번호 0001"))
        XCTAssertTrue(text("아직 촬영한 사진이 없어요.").waitForExistence(timeout: 10))
        let home = XCTAttachment(screenshot: app.screenshot())
        home.name = "shared-home-empty"
        home.lifetime = .keepAlways
        add(home)
        openHomeTemplates()
        let listScreen = XCTAttachment(screenshot: app.screenshot())
        listScreen.name = "shared-template-list"
        listScreen.lifetime = .keepAlways
        add(listScreen)
        XCTAssertFalse(app.buttons["+ 새 템플릿"].exists, "PoC must not expose unavailable editing")
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
        app.launchArguments = ["--home-navigation"]
        app.launch()
        tapButton("저장 정보")
        XCTAssertTrue(text("다음 촬영 저장 정보").waitForExistence(timeout: 10))
        XCTAssertTrue(text("ios-sample_0001").waitForExistence(timeout: 10))
        XCTAssertFalse(text("ios-sample_0001.jpg").exists, "Keep existing extension-free display")
        XCTAssertTrue(text("PoC/현장 기록/").exists)
        XCTAssertTrue(text("PoC/보관/").exists)
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
        XCTAssertTrue(text("ios-sample_0001").exists)
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
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "iosqa")).firstMatch.exists,
                      "Edited contents must remain in the table after keyboard dismissal")
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
        tapPanelText("파일명 값")
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
        XCTAssertTrue(try screenContains("iosqa"), "Saved cell text must survive app relaunch")
        tapPanelText("구조")
        XCTAssertTrue(try screenContains("iosqa"), "Structural change must retain edited contents")
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
