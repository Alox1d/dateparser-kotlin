import SwiftUI

@main
struct DateParserDemoApp: App {
    @StateObject private var model: ReminderViewModel

    init() {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--demo-fixed-reference") {
            let reference = ISO8601DateFormatter().date(from: "2026-07-03T10:15:00Z")!
            _model = StateObject(wrappedValue: ReminderViewModel(
                now: { reference }, initialTimeZone: TimeZone(identifier: "UTC")!
            ))
            return
        }
        #endif
        _model = StateObject(wrappedValue: ReminderViewModel())
    }

    var body: some Scene {
        WindowGroup { ReminderView(model: model) }
    }
}
