import XCTest
@testable import DateParserDemo

@MainActor
final class ReminderViewModelTests: XCTestCase {
    private let reference = ISO8601DateFormatter().date(from: "2026-07-03T10:15:00Z")!

    private func makeModel() -> ReminderViewModel {
        ReminderViewModel(now: { self.reference }, initialTimeZone: TimeZone(identifier: "UTC")!)
    }

    func testConfirmKeepsEditedTitleAndBothRangeBoundaries() {
        let model = makeModel()
        model.input = "Buy milk, tomorrow from 5PM to 8PM"
        model.preview()
        XCTAssertTrue(model.hasPreview)
        XCTAssertTrue(model.includesEnd)
        model.title = "  Groceries  "
        model.confirm()
        XCTAssertEqual(model.reminders.count, 1)
        XCTAssertEqual(model.reminders[0].title, "Groceries")
        XCTAssertEqual(model.reminders[0].startsAt, date("2026-07-04T17:00:00Z"))
        XCTAssertEqual(model.reminders[0].endsAt, date("2026-07-04T20:00:00Z"))
        XCTAssertEqual(model.reminders[0].timeZoneId, "UTC")
        XCTAssertFalse(model.hasPreview)
    }

    func testInputAndParsingOptionsInvalidateThePreviousPreview() {
        let model = makeModel()
        model.preview()
        model.input = "Call tomorrow at 8pm"
        XCTAssertFalse(model.canConfirm)
        model.preview()
        model.timeZoneId = "Europe/Berlin"
        XCTAssertFalse(model.canConfirm)
        model.preview()
        model.dateOrder = "DMY"
        XCTAssertFalse(model.canConfirm)
        model.confirm()
        XCTAssertTrue(model.reminders.isEmpty)
    }

    func testRussianInputUsesTheSameSharedParser() {
        let model = makeModel()
        model.input = "купить через 5 часов"
        model.preview()
        XCTAssertEqual(model.title, "купить")
        XCTAssertEqual(model.startsAt, date("2026-07-03T15:15:00Z"))
        XCTAssertFalse(model.includesEnd)
    }

    func testAmbiguousInputNeedsAnExplicitDateOrder() {
        let model = makeModel()
        model.input = "03/04/2026"
        model.preview()
        XCTAssertFalse(model.hasPreview)
        model.dateOrder = "DMY"
        model.preview()
        XCTAssertEqual(model.startsAt, date("2026-04-03T10:15:00Z"))
        XCTAssertFalse(model.canConfirm) // A date-only phrase still needs a title.
        model.title = "Appointment"
        XCTAssertTrue(model.canConfirm)
    }

    func testEndMustBeAfterStartAndTitleMustBePresent() {
        let model = makeModel()
        model.preview()
        model.includesEnd = true
        model.endsAt = model.startsAt
        XCTAssertFalse(model.canConfirm)
        model.endsAt = model.startsAt.addingTimeInterval(1800)
        XCTAssertTrue(model.canConfirm)
        model.title = "  "
        XCTAssertFalse(model.canConfirm)
    }

    func testUnsupportedInputCanBeReviewedManually() {
        let model = makeModel()
        model.input = "Something without a date"
        model.preview()
        XCTAssertFalse(model.hasPreview)
        model.beginManualReview()
        model.startsAt = date("2026-07-05T12:00:00Z")
        model.confirm()
        XCTAssertEqual(model.reminders.count, 1)
        XCTAssertEqual(model.reminders[0].startsAt, date("2026-07-05T12:00:00Z"))
        XCTAssertNil(model.reminders[0].endsAt)
    }

    func testAddingAnEndUsesThirtyMinutesFromTheReviewedStart() {
        let model = makeModel()
        model.preview()
        model.startsAt = model.startsAt.addingTimeInterval(-3600)
        model.setIncludesEnd(true)
        XCTAssertEqual(model.endsAt.timeIntervalSince(model.startsAt), 1800)
    }

    func testRecurringInputIsNotAutomaticallySavedAsOneReminder() {
        let model = makeModel()
        model.input = "every 2 days"
        model.preview()
        XCTAssertFalse(model.hasPreview)
        model.confirm()
        XCTAssertTrue(model.reminders.isEmpty)
    }

    private func date(_ value: String) -> Date {
        ISO8601DateFormatter().date(from: value)!
    }
}
