# Published KMP reminder demo

This standalone Kotlin Multiplatform consumer uses **DateParser Kotlin 0.1.3 from Maven Central**. Its reminder preview lives in `src/commonMain`; the same code compiles for JVM, Android API 26+, and iOS arm64/device and simulator. A small JVM entry point prints the results without requiring an emulator.

From the repository root:

```sh
./gradlew -p integration/consumer runDemo -PcentralOnly=true --console=plain
```

Build prerequisites: JDK 17-21 and Android SDK 35. Full Xcode on macOS is needed only for the iOS checks. The repository's Gradle wrapper is used; no separate Gradle installation is necessary.

For a terminal example that needs **no Android SDK**, use the separate [JVM-only consumer](../jvm-consumer/README.md): `./gradlew -p integration/jvm-consumer run`. It also resolves 0.1.3 from Maven Central.

For an interactive iOS form, open the [SwiftUI host](../ios-app/README.md). This shared module builds its `ReminderShared` framework locally; `ReminderPreviewParser` exposes primitive values to Swift and delegates to the same `parseReminderDraft` policy. The host requires an Apple Silicon Mac and full Xcode.

The fixed reference is `2026-07-03T10:15:00Z`. The demo covers named US/European dates, AM/PM, reminder text, range boundaries, timezone conversion, and explicit MDY/DMY. An unresolved ambiguous date or a recurring interval goes to manual review. This example does not save or schedule a reminder.

Try your own input:

```sh
./gradlew -p integration/consumer runDemo -PcentralOnly=true \
  '-PdemoText=Meet on 4th July 2026 at 5 pm' \
  -PdemoTimeZone=Europe/Berlin --console=plain

./gradlew -p integration/consumer runDemo -PcentralOnly=true \
  -PdemoText=03/04/2026 -PdemoDateOrder=DMY --console=plain
```

Optional properties: `demoReference` (ISO instant), `demoTimeZone` (IANA zone), and `demoDateOrder` (`NONE`, `MDY`, `DMY`, or `YMD`; default `NONE`). Dates without a clock inherit the reference clock. The example does not automatically move past dates into the future.

Verify the shared preview against the public artifacts:

```sh
./gradlew -p integration/consumer jvmTest testAndroidHostTest -PcentralOnly=true
# On macOS with full Xcode:
./gradlew -p integration/consumer iosSimulatorArm64Test compileKotlinIosArm64 -PcentralOnly=true
```

Without `centralOnly=true`, this consumer first tries the root project's unsigned `build/test-repository` for library development. Keep the flag when following the public demo.

Read the [KMP walkthrough](https://alox1d.github.io/dateparser-kotlin/en/kmp-reminder-demo/) for installation, shared code, user confirmation, and US/EU date handling.

For the interactive Android Compose preview, run `./gradlew :app:installDebug` from the root build and open DateParser. The Android screen compiles this consumer's `src/commonMain/kotlin/ReminderDraft.kt` unchanged, but uses the local library project to preview unreleased parser changes. It retains both range endpoints and can pass a reviewed event to the system calendar editor. A single instant requires an explicit end before calendar export; saving a reminder in the demo still keeps it only for the session. Real emulator screenshots and the calendar handoff policy are linked from the [main README](../../README.md#android-compose-reminder-demo).
