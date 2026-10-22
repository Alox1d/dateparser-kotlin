---
title: Parse reminder dates in Kotlin Multiplatform - an offline KMP walkthrough
description: Build a shared reminder preview with DateParser Kotlin 0.1.3 from Maven Central. Handle US and European date order, timezones and unsupported input.
---

# Parse reminder dates in shared Kotlin code

A reminder field is more useful when it accepts `Buy milk, tomorrow from 5PM to 8PM`. The application still needs an editable title, an actual timestamp, and a clear way to ask for clarification. For a Kotlin Multiplatform app, this conversion can live in `commonMain`, while Android and iOS own the input form and confirmation screen.

This walkthrough uses **DateParser Kotlin 0.1.3**, an MIT library that recognizes supported English and Russian date expressions offline. You will build a small reminder preview, run it on JVM, and use the same shared code on Android and iOS. The standalone consumers and SwiftUI host use the published Maven Central dependency, so they also check real consumer integration.

## Run the example first

Clone the repository and run the existing standalone KMP consumer:

```sh
git clone https://github.com/Alox1d/dateparser-kotlin.git
cd dateparser-kotlin
./gradlew -p integration/consumer runDemo -PcentralOnly=true --console=plain
```

If you want to try the published parser on **JVM without installing Android SDK**, the independent JVM consumer is a shorter first step:

```sh
./gradlew -p integration/jvm-consumer run --console=plain
./gradlew -p integration/jvm-consumer run --args='--help' --console=plain
```

That build uses only Kotlin/JVM and Maven Central. It includes fixed English/Russian examples and custom options for reference, timezone and date order. A custom no-match returns application status 1; invalid options return 2 with an explanation. The [JVM README](https://github.com/Alox1d/dateparser-kotlin/blob/main/integration/jvm-consumer/README.md) includes commands for your own input.

Use JDK 17-21 for either build. The shared KMP consumer additionally needs Android SDK 35 because it declares an Android target. It pins Kotlin 2.3.20, Android Gradle Plugin 8.13.2 and Gradle 8.13. Both terminal demos run without an emulator. Full Xcode on macOS is required for the iOS host and native checks.

Here is part of the real output with the fixed reference `2026-07-03T10:15:00Z`:

```text
Input: Buy milk, tomorrow from 5PM to 8PM
Zone: UTC | Slash order: not selected
Title: Buy milk
Start: 2026-07-04T17:00 | UTC: 2026-07-04T17:00:00Z
End: 2026-07-04T20:00 | UTC: 2026-07-04T20:00:00Z
Preview only: confirm or edit before saving.
```

The input stays on the device. A fixed reference makes this result reproducible; your application should pass the current instant when the user submits the field.

## Add the dependency to commonMain

In an existing shared KMP module, make Maven Central available and add:

```kotlin
repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.alox1d:dateparser-kotlin:0.1.3")
        }
    }
}
```

Gradle chooses the matching platform artifact. The checked-in [consumer build](https://github.com/Alox1d/dateparser-kotlin/blob/main/integration/consumer/build.gradle.kts) includes JVM, Android and both iOS targets. Android requires API 26 or later. The library supports shared Kotlin iOS code; this release does not provide a standalone Swift/XCFramework package. A Java 8 runtime is supported for the JVM library, although the Gradle build itself requires a newer JDK.

## Turn one match into an editable draft

Keep this application code in `commonMain`:

```kotlin
import io.github.alox1d.dateparser.DateOrder
import io.github.alox1d.dateparser.DateParser
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

data class ReminderDraft(
    val title: String,
    val startsAt: Instant,
    val endsAt: Instant?,
    val timeZone: TimeZone
)

fun parseReminderDraft(
    text: String,
    reference: Instant,
    timeZone: TimeZone,
    dateOrder: DateOrder? = null
): ReminderDraft? {
    val parser = DateParser()
    val matches = if (dateOrder == null) {
        parser.parse(text, reference, timeZone)
    } else {
        parser.parse(text, dateOrder, reference, timeZone)
    }
    val match = matches.singleOrNull() ?: return null
    // A recurring interval needs its own scheduling UI and policy.
    if (match.recurrence != null) return null
    return ReminderDraft(
        match.remainingText, match.instant, match.endInstant, match.timeZone
    )
}
```

This is the sample application's policy: accept one non-recurring match as a **draft**, and return `null` when it needs manual review. The library itself can return multiple matches and recurrence intervals. If your app supports recurring reminders, handle `DateMatch.recurrence` explicitly in its scheduling flow instead of discarding it.

Display the draft's date, time, timezone and title in editable fields. An empty title needs a title from the user. On `null`, retain the original input and offer a date picker or a more specific question. A rule-based parser can recognize a portion of a longer message, so even a successful result should be confirmed before saving. Its confidence score is a rule score, not a probability that the user's intent was understood.

The [shared source](https://github.com/Alox1d/dateparser-kotlin/blob/main/integration/consumer/src/commonMain/kotlin/ReminderDraft.kt) and [JVM runner](https://github.com/Alox1d/dateparser-kotlin/blob/main/integration/consumer/src/jvmMain/kotlin/Demo.kt) are available in the repository.

## Make US and European date order explicit

Named dates avoid numeric ambiguity. Both `July 4, 2026 at 5PM` and `4th July 2026 at 5 pm` resolve to July 4 at 17:00 in the selected timezone.

The numeric input `03/04/2026` needs a choice:

| Caller selection | Resolved date |
| --- | --- |
| `DateOrder.MDY` | March 4, 2026 |
| `DateOrder.DMY` | April 3, 2026 |
| No selection | No match; ask the user |

Use an explicit input preference or a user-selected format. A timezone does not specify a numeric date order. The demo makes the preference visible:

```sh
./gradlew -p integration/consumer runDemo -PcentralOnly=true \
  -PdemoText=03/04/2026 -PdemoDateOrder=DMY --console=plain
```

Prefer four-digit years in your form. Selecting `DateOrder` does not expand a short year such as `26` into `2026`. Dates without a clock inherit the reference clock; if your product needs all-day reminders, define that policy separately. The example leaves past dates unchanged, rather than silently moving them into the future.

## Pass a timezone as well as a reference instant

The same local clock represents different moments in different places. For July 4, 2026 at 17:00:

| Zone | UTC instant |
| --- | --- |
| `Europe/Berlin` | `2026-07-04T15:00:00Z` |
| `America/New_York` | `2026-07-04T21:00:00Z` |

Try the European named-date form in Berlin:

```sh
./gradlew -p integration/consumer runDemo -PcentralOnly=true \
  '-PdemoText=Meet on 4th July 2026 at 5 pm' \
  -PdemoTimeZone=Europe/Berlin --console=plain
```

Store the resolved instant and the timezone needed for display. In production, `Clock.System.now()` can supply the reference, and the user's chosen zone can supply `TimeZone`. DateParser uses elapsed time for relative hours/minutes/seconds and calendar arithmetic for days/months/years. A calendar day is not always 24 elapsed hours across a daylight-saving transition. Show the resolved local time in the preview, especially around DST gaps and overlaps.

## Review the shared result in SwiftUI

The repository includes an [iOS host with a checked-in Xcode project](https://github.com/Alox1d/dateparser-kotlin/blob/main/integration/ios-app/README.md). On an Apple Silicon Mac, open `integration/ios-app/DateParserDemo.xcodeproj`, choose the `DateParserDemo` scheme and an iPhone simulator, then run. Its build phase resolves DateParser Kotlin 0.1.3 from Maven Central and builds a local static framework from the shared consumer. No CocoaPods or project-generation step is needed.

The application adapter exposes a small boundary to Swift:

```swift
import Foundation
import ReminderShared

let preview = ReminderPreviewParser().parse(
    text: "Buy milk tomorrow at 7pm",
    referenceEpochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000),
    timeZoneId: "Europe/Berlin",
    dateOrder: "NONE"
)
```

`ReminderPreviewParser` delegates to `parseReminderDraft`; it exposes the title, start/end epoch milliseconds and timezone ID. Date parsing stays in Kotlin. The SwiftUI form turns those values into editable date pickers, displays the corresponding UTC instants and requires explicit confirmation. Changing the original input, timezone or date order invalidates the previous preview. Both range endpoints are retained, an empty title needs user input, and an end before the start blocks confirmation.

Confirmed values live only in the session list. The host adds no persistence or notification scheduling. It is a source integration example, so users build the local framework as part of the app; the library release still has no separate Swift package distribution.

## Pass a reviewed event to the Android calendar editor

The [Android Compose example](https://github.com/Alox1d/dateparser-kotlin/tree/main/app) has separate actions for keeping a reminder in its session list and opening a calendar event. This app compiles the same shared reminder adapter but depends on the local `:dateparser` project; the standalone consumer checks the published Android artifact. A parsed range provides both endpoints. A single date/time needs an explicit 30-minute or one-hour choice before calendar export; the end remains editable afterward.

After validating the reviewed values, the sample builds the standard Android insert Intent:

```kotlin
val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
    putExtra(CalendarContract.Events.TITLE, reviewedTitle)
    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, reviewedStart.toEpochMilliseconds())
    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, reviewedEnd.toEpochMilliseconds())
    putExtra(CalendarContract.Events.EVENT_TIMEZONE, reviewedZone.id)
    putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
}
```

The [complete implementation](https://github.com/Alox1d/dateparser-kotlin/blob/main/app/src/main/java/com/example/dateparser/ReminderScreen.kt) opens that Intent and handles `ActivityNotFoundException` without discarding the review. The calendar app owns the final Save/Cancel step, following Android's [calendar insert Intent](https://developer.android.com/identity/providers/calendar-provider#intent-insert) flow. The demo requests no calendar permission and does not write directly to a calendar provider.

An unchanged parsed endpoint retains its original instant. A manually edited local endpoint must have exactly one valid UTC offset; DST gaps and overlaps require another selection. Start and end are checked independently, and the end must be after the start. The application does not manufacture a recurrence schedule from a parser interval.

## Check all the platforms you use

The sample's shared behavior tests exercise reminder titles, both range boundaries, explicit date order, timezone resolution, unsupported input and recurrence handling. Run them against Maven Central:

```sh
./gradlew -p integration/consumer jvmTest testAndroidHostTest -PcentralOnly=true
# On macOS with full Xcode:
./gradlew -p integration/consumer iosSimulatorArm64Test compileKotlinIosArm64 -PcentralOnly=true
# SwiftUI state and UI tests on an available iPhone simulator:
bash scripts/test_ios_demo.sh
```

The independent JVM consumer has its own `test` task. Android calendar payload tests run with `./gradlew :app:connectedDebugAndroidTest` and `ANDROID_SERIAL` set to a test device; they inspect the actual Intent and do not save an event in a calendar app. The [sample READMEs](https://github.com/Alox1d/dateparser-kotlin/tree/main/integration) document prerequisites and what each check covers.

Android host tests validate shared code on the JVM; they are not device instrumentation tests. The iOS simulator tests execute the shared code, while device compilation checks the other native target. Your platform code still owns permissions, notifications, persistence and background scheduling.

## Use a parser with a clear boundary

DateParser Kotlin is a small offline rule engine with tested forms. It is useful for a reminder input that has a confirmation step. It does not promise to understand arbitrary prose, provide source character spans, assign semantic roles to every date in an SMS, or implement a complete recurrence scheduler. Keep manual entry available and add representative phrases from your own users to your tests.

See the [API guide](index.md), [Maven Central release](https://central.sonatype.com/artifact/io.github.alox1d/dateparser-kotlin/0.1.3), and [complete runnable example](https://github.com/Alox1d/dateparser-kotlin/tree/main/integration/consumer). If this helps your Kotlin project, feedback about real English inputs and integrations is welcome; a [GitHub star](https://github.com/Alox1d/dateparser-kotlin) also helps others discover the library.
