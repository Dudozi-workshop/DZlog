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

    func testTemplateListPreviewSelectionAndBack() {
        app.launchArguments = ["--template-navigation"]
        app.launch()
        tapButton("템플릿 목록")
        XCTAssertTrue(text("표 템플릿").waitForExistence(timeout: 10))
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
        XCTAssertTrue(text("선택한 템플릿 · 현장 기록").waitForExistence(timeout: 10))
        tapButton("템플릿 목록")
        XCTAssertTrue(app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS %@", "사용 중")).firstMatch
            .waitForExistence(timeout: 10))
        tapButton("뒤로가기")
        XCTAssertTrue(text("선택한 템플릿 · 현장 기록").waitForExistence(timeout: 10))
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
