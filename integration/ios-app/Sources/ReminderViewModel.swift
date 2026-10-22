import Combine
import Foundation
import ReminderShared

struct ConfirmedReminder: Identifiable {
    let id = UUID()
    let title: String
    let startsAt: Date
    let endsAt: Date?
    let timeZoneId: String
}

@MainActor
final class ReminderViewModel: ObservableObject {
    @Published var input = "Buy milk tomorrow at 7pm" {
        didSet { if input != oldValue { invalidatePreview() } }
    }
    @Published var dateOrder = "NONE" {
        didSet { if dateOrder != oldValue { invalidatePreview() } }
    }
    @Published var timeZoneId: String {
        didSet { if timeZoneId != oldValue { invalidatePreview() } }
    }
    @Published var title = ""
    @Published var startsAt: Date
    @Published var endsAt: Date
    @Published var includesEnd = false
    @Published private(set) var hasPreview = false
    @Published private(set) var message = ""
    @Published private(set) var reminders: [ConfirmedReminder] = []

    private let parser = ReminderPreviewParser()
    private let now: () -> Date

    init(now: @escaping () -> Date = Date.init, initialTimeZone: TimeZone = .current) {
        let initialDate = now()
        self.now = now
        self.timeZoneId = initialTimeZone.identifier
        self.startsAt = initialDate
        self.endsAt = initialDate.addingTimeInterval(30 * 60)
    }

    var displayTimeZone: TimeZone { TimeZone(identifier: timeZoneId) ?? .gmt }

    var canConfirm: Bool {
        hasPreview && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && TimeZone(identifier: timeZoneId) != nil && (!includesEnd || endsAt > startsAt)
    }

    func preview() {
        guard let result = parser.parse(
            text: input,
            referenceEpochMilliseconds: Int64(now().timeIntervalSince1970 * 1_000),
            timeZoneId: timeZoneId,
            dateOrder: dateOrder
        ) else {
            hasPreview = false
            message = "No single, unambiguous date found. Try 7pm or 19:00, select a date order, or enter a one-off date manually. Recurring reminders need a separate flow."
            return
        }
        title = result.title
        startsAt = Date(timeIntervalSince1970: Double(result.startEpochMilliseconds) / 1_000)
        if let end = result.endEpochMilliseconds {
            endsAt = Date(timeIntervalSince1970: Double(end.int64Value) / 1_000)
            includesEnd = true
        } else {
            includesEnd = false
            endsAt = startsAt.addingTimeInterval(30 * 60)
        }
        hasPreview = true
        message = "Review the resolved time and title before confirming."
    }

    func useExample(_ text: String) {
        input = text
        preview()
    }

    func beginManualReview() {
        title = input.trimmingCharacters(in: .whitespacesAndNewlines)
        startsAt = now()
        endsAt = startsAt.addingTimeInterval(30 * 60)
        includesEnd = false
        hasPreview = true
        message = "Choose the date, time and title for this one-off reminder."
    }

    func setIncludesEnd(_ included: Bool) {
        if included && !includesEnd {
            endsAt = startsAt.addingTimeInterval(30 * 60)
        }
        includesEnd = included
    }

    func confirm() {
        guard canConfirm else { return }
        reminders.insert(ConfirmedReminder(
            title: title.trimmingCharacters(in: .whitespacesAndNewlines),
            startsAt: startsAt,
            endsAt: includesEnd ? endsAt : nil,
            timeZoneId: timeZoneId
        ), at: 0)
        input = ""
        hasPreview = false
        message = "Saved for this session. No notification is scheduled."
    }

    private func invalidatePreview() {
        hasPreview = false
        message = ""
    }
}
