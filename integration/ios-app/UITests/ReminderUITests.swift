import XCTest

final class ReminderUITests: XCTestCase {
    private func launch() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["--demo-fixed-reference"]
        app.launch()
        return app
    }

    func testEnglishPreviewRequiresConfirmation() {
        let app = launch()
        app.buttons["parse_reminder"].tap()
        XCTAssertTrue(app.textFields["reminder_title"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.textFields["reminder_title"].value as? String, "Buy milk")
        let confirm = app.buttons["confirm_reminder"]
        if !confirm.isHittable { app.swipeUp() }
        confirm.tap()
        XCTAssertTrue(app.staticTexts["Saved for this session. No notification is scheduled."].exists)
        XCTAssertTrue(app.staticTexts["Buy milk"].exists)
    }

    func testRussianExampleAndRangeUseTheSharedPreview() {
        let app = launch()
        // Tap the visible Menu label, not the wider Form row accessibility wrapper.
        let examples = app.staticTexts["Try an example"]
        XCTAssertTrue(examples.waitForExistence(timeout: 10))
        examples.tap()
        let russian = app.descendants(matching: .any).matching(identifier: "Russian").firstMatch
        XCTAssertTrue(russian.waitForExistence(timeout: 10))
        russian.tap()
        XCTAssertTrue(app.textFields["reminder_title"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.textFields["reminder_title"].value as? String, "купить")
        if !examples.isHittable { app.swipeDown() }
        examples.tap()
        let range = app.descendants(matching: .any).matching(identifier: "Range").firstMatch
        XCTAssertTrue(range.waitForExistence(timeout: 10))
        range.tap()
        let includeEnd = app.switches["include_end"]
        if !includeEnd.isHittable { app.swipeUp() }
        XCTAssertTrue(includeEnd.waitForExistence(timeout: 10))
        XCTAssertEqual(includeEnd.value as? String, "1")
    }
}
