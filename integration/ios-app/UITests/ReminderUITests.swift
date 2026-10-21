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
        app.buttons["try_example"].tap()
        app.buttons["Russian"].tap()
        XCTAssertTrue(app.textFields["reminder_title"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.textFields["reminder_title"].value as? String, "купить")
        app.buttons["try_example"].tap()
        app.buttons["Range"].tap()
        XCTAssertTrue(app.switches["include_end"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.switches["include_end"].value as? String, "1")
    }
}
