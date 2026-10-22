# SwiftUI host for the published KMP consumer

An iOS reminder form calls the same `parseReminderDraft` policy used by the Android demo. The shared Kotlin module resolves **DateParser Kotlin 0.1.3 from Maven Central** and builds a local static `ReminderShared` framework for this app. This is a source example, not a separately published Swift package or XCFramework distribution.

## Run

Use an Apple Silicon Mac, full Xcode with an iOS Simulator runtime, JDK 17-21, and Android SDK 35. The SDK is needed because the existing shared consumer also declares an Android target. The app supports iOS 16 or later.

From the repository root:

```sh
export JAVA_HOME=$(/usr/libexec/java_home)
export ANDROID_HOME="$HOME/Library/Android/sdk" # Adjust if your SDK is elsewhere.
open integration/ios-app/DateParserDemo.xcodeproj
```

Select the **DateParserDemo** scheme and an iPhone simulator, then Run. Xcode's build phase runs the repository Gradle wrapper with `-PcentralOnly=true`; no CocoaPods or XcodeGen setup is needed. If Xcode was already open, ensure its Gradle build can see `JAVA_HOME` and the Android SDK. An untracked `integration/consumer/local.properties` containing `sdk.dir=/absolute/path/to/sdk` is another way to supply the SDK path. Device builds require your own signing team in Xcode.

To check compilation without a booted simulator:

```sh
xcodebuild -project integration/ios-app/DateParserDemo.xcodeproj \
  -scheme DateParserDemo -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath integration/ios-app/build/DerivedData \
  CODE_SIGNING_ALLOWED=NO build-for-testing
```

## Review flow

1. Enter an English or Russian phrase, or choose an example.
2. Select a timezone and, for numeric dates, an explicit MDY/DMY/YMD order. **Ask** leaves the order unspecified.
3. Preview the parsed title and exact date/time. A range keeps both endpoints. The UTC values identify the absolute instants; the date pickers display the selected timezone.
4. Edit the title, start and optional end. Enabling an end for a single instant starts with an explicit, visible 30-minute choice. End must be after start.
5. Confirm to add the details to the in-memory session list.

Changing the original phrase, timezone or date order invalidates the previous preview. Empty titles and invalid ranges cannot be confirmed. Unsupported/ambiguous input and recurrence remain in review; manual entry explicitly creates a one-off reminder. Dates without a time keep the reference clock, and past dates are not moved into the future.

The app sends no network requests for parsing. It does not persist reminders, create calendar events, or schedule notifications. Restarting it loses the session list.

## Tests

```sh
# Shared parsing policy and the primitive-value Swift adapter:
./gradlew -p integration/consumer jvmTest -PcentralOnly=true

# Native execution, with an iOS Simulator runtime installed:
./gradlew -p integration/consumer iosSimulatorArm64Test -PcentralOnly=true
bash scripts/test_ios_demo.sh
```

The script selects an available iPhone simulator. Set `IOS_SIMULATOR_ID` to choose a specific one. It runs eight view-model tests and two UI scenarios, including EN/RU preview, range retention and explicit confirmation. UI tests use a fixed debug reference; normal app launches use the current instant. Each run writes a timestamped result bundle under `integration/ios-app/build`; set `IOS_TEST_RESULTS` to choose another new path.

`ReminderPreviewParser` is an application adapter exposing strings and epoch milliseconds to Swift. The parser implementation remains in the published Kotlin dependency; the app does not reimplement date parsing in Swift.
