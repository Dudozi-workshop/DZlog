import XCTest

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
        XCTAssertTrue(element.isHittable, "Button obscured: \(label)")
        // Native screen bounds check is a basic Safe Area sanity check, not full layout approval.
        XCTAssertTrue(app.frame.contains(element.frame), "Button outside screen: \(label)")
        return element
    }

    func testCloseReopenSelectAndRetainSelection() {
        app.launch()
        button("닫기").tap()
        XCTAssertTrue(text("템플릿을 선택해 주세요.").waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons["이 템플릿 선택"].exists)
        button("표 미리보기").tap()
        button("이 템플릿 선택").tap()
        XCTAssertTrue(text("선택한 템플릿 · 현장 기록").waitForExistence(timeout: 10))
        button("표 미리보기").tap()
        button("닫기").tap()
        XCTAssertTrue(text("선택한 템플릿 · 현장 기록").waitForExistence(timeout: 10))
    }

    func testHorizontalGestureRevealsLastColumn() {
        app.launchArguments = ["--large-table"]
        app.launch()
        XCTAssertTrue(text("왼쪽 시작").waitForExistence(timeout: 20))
        let target = text("오른쪽 끝")
        XCTAssertFalse(target.isHittable, "Fixture must overflow horizontally")
        let y = text("왼쪽 시작").frame.midY
        for _ in 0..<10 {
            if target.isHittable { break }
            app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: app.frame.width * 0.78, dy: y))
                .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: .zero)
                    .withOffset(CGVector(dx: app.frame.width * 0.22, dy: y)))
        }
        XCTAssertTrue(target.isHittable, "Horizontal gesture did not reveal last column")
        button("닫기").tap()
        XCTAssertTrue(text("템플릿을 선택해 주세요.").waitForExistence(timeout: 10))
    }

    func testVerticalGestureRevealsLastRow() {
        app.launchArguments = ["--large-table"]
        app.launch()
        XCTAssertTrue(text("왼쪽 시작").waitForExistence(timeout: 20))
        let target = text("마지막 행")
        XCTAssertFalse(target.isHittable, "Fixture must overflow vertically")
        let top = text("왼쪽 시작").frame.midY
        for _ in 0..<12 {
            if target.isHittable { break }
            app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: app.frame.width * 0.4, dy: top + 240))
                .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: .zero)
                    .withOffset(CGVector(dx: app.frame.width * 0.4, dy: top)))
        }
        XCTAssertTrue(target.isHittable, "Vertical gesture did not reveal last row")
        button("이 템플릿 선택").tap()
        XCTAssertTrue(text("선택한 템플릿 · 스크롤 검증").waitForExistence(timeout: 10))
    }
}
