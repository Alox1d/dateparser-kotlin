import SwiftUI

struct ReminderView: View {
    @ObservedObject var model: ReminderViewModel
    @FocusState private var inputFocused: Bool

    private var timeZones: [String] {
        Array(Set([TimeZone.current.identifier, "UTC", "Europe/Berlin", "America/New_York"])).sorted()
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("DATEPARSER KOTLIN")
                            .font(.caption.weight(.bold)).tracking(1.5)
                        Text("Make time for it.")
                            .font(.system(size: 34, weight: .semibold, design: .serif))
                        Text("Offline EN + RU · Review before confirming")
                            .font(.subheadline)
                    }
                    .foregroundStyle(.purple).padding(.vertical, 8)
                }

                Section("Write a reminder") {
                    TextField("Buy milk tomorrow at 7pm", text: $model.input, axis: .vertical)
                        .lineLimit(2...4).focused($inputFocused)
                        .accessibilityIdentifier("reminder_input")
                    Picker("Timezone", selection: $model.timeZoneId) {
                        ForEach(timeZones, id: \.self) { Text($0).tag($0) }
                    }
                    Picker("Numeric date order", selection: $model.dateOrder) {
                        Text("Ask").tag("NONE")
                        Text("MDY").tag("MDY")
                        Text("DMY").tag("DMY")
                        Text("YMD").tag("YMD")
                    }
                    .pickerStyle(.segmented)
                    Text("03/04/2026 needs a date order. Dates without a clock keep the current reference time.")
                        .font(.caption).foregroundStyle(.secondary)
                    Button("Preview reminder") {
                        inputFocused = false
                        model.preview()
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(model.input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityIdentifier("parse_reminder")
                    Menu("Try an example") {
                        Button("English") { useExample("Buy milk tomorrow at 7pm") }
                        Button("Russian") { useExample("купить через 5 часов") }
                        Button("Range") { useExample("Work tomorrow from 5PM to 8PM") }
                        Button("Ambiguous date") { useExample("03/04/2026") }
                    }
                    .accessibilityIdentifier("try_example")
                }

                if !model.message.isEmpty {
                    Section {
                        Text(model.message).font(.subheadline)
                            .accessibilityIdentifier("review_message")
                    }
                }

                if model.hasPreview {
                    Section("Review your reminder") {
                        TextField("Reminder title", text: $model.title)
                            .accessibilityIdentifier("reminder_title")
                        DatePicker("Start", selection: $model.startsAt)
                            .accessibilityIdentifier("reminder_start")
                        utcLabel(model.startsAt)
                        Toggle("Include an end", isOn: Binding(
                            get: { model.includesEnd }, set: model.setIncludesEnd
                        ))
                            .accessibilityIdentifier("include_end")
                        if model.includesEnd {
                            DatePicker("End", selection: $model.endsAt)
                                .accessibilityIdentifier("reminder_end")
                            utcLabel(model.endsAt)
                            if model.endsAt <= model.startsAt {
                                Text("Choose an end after the start, including its date for an overnight range.")
                                    .foregroundStyle(.red).font(.caption)
                            }
                        } else {
                            Text("Adding an end starts with 30 minutes. Review both values before confirming.")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                        Text("Shown in \(model.timeZoneId). The UTC values identify the exact moments, including daylight-saving transitions.")
                            .font(.caption).foregroundStyle(.secondary)
                        Button("Confirm reminder") {
                            inputFocused = false
                            model.confirm()
                        }
                        .disabled(!model.canConfirm)
                        .accessibilityIdentifier("confirm_reminder")
                    }
                    .environment(\.timeZone, model.displayTimeZone)
                } else {
                    Section {
                        Button("Choose a one-off date manually") {
                            inputFocused = false
                            model.beginManualReview()
                        }
                    }
                }

                Section("Your reminders · this session") {
                    if model.reminders.isEmpty {
                        Text("Confirmed reminders appear here.").foregroundStyle(.secondary)
                    }
                    ForEach(model.reminders) { reminder in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(reminder.title).font(.headline)
                            Text(localDate(reminder.startsAt, zoneId: reminder.timeZoneId))
                            if let end = reminder.endsAt {
                                Text("Until \(localDate(end, zoneId: reminder.timeZoneId))")
                            }
                            Text(reminder.timeZoneId).font(.caption).foregroundStyle(.secondary)
                        }
                        .font(.subheadline)
                    }
                    Text("This demo keeps confirmed details in memory. Closing the app loses them. It does not schedule notifications or save calendar events.")
                        .font(.caption).foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Reminder preview")
            .navigationBarTitleDisplayMode(.inline)
            .scrollDismissesKeyboard(.interactively)
            .tint(.purple)
        }
    }

    private func useExample(_ text: String) {
        inputFocused = false
        model.useExample(text)
    }

    private func utcLabel(_ date: Date) -> some View {
        Text("UTC: \(ISO8601DateFormatter().string(from: date))")
            .font(.caption.monospaced()).foregroundStyle(.secondary)
    }

    private func localDate(_ date: Date, zoneId: String) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        formatter.timeZone = TimeZone(identifier: zoneId)
        return formatter.string(from: date)
    }
}
