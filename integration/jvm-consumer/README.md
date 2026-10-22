# JVM consumer of DateParser Kotlin

A small Kotlin/JVM application that uses **`io.github.alox1d:dateparser-kotlin:0.1.3` from Maven Central**. It has its own Gradle settings and uses the Kotlin JVM and application plugins. It requires **JDK 17-21**, with no Android SDK, Xcode, or local library publication. The repository supplies Gradle 8.13 through its wrapper.

## Run the examples

From the repository root:

```sh
./gradlew -p integration/jvm-consumer run --console=plain
```

In Windows PowerShell, replace `./gradlew` with `.\gradlew.bat`.

The examples use the fixed reference instant **`2026-07-03T10:15:00Z`** and print the timezone and numeric date order for each call. They cover English and Russian, named dates, a reminder with both range boundaries, timezone conversion, and explicit MDY/DMY interpretation. Ambiguous and unsupported inputs are labeled with `Expected: NO MATCH`; the example suite fails if a result disagrees with its expectation.

Recurring input such as `every 2 days` is displayed as a calendar interval, with its parser output. The application does not calculate a schedule, save reminders, or send notifications. A date without a time inherits the reference clock in the selected timezone. An explicit past date remains in the past.

## Try your own text

```sh
./gradlew -p integration/jvm-consumer run --console=plain \
  '-PdemoText=Meet on 4th July 2026 at 5 pm' \
  -PdemoTimeZone=Europe/Berlin \
  -PdemoReference=2026-07-03T10:15:00Z

./gradlew -p integration/jvm-consumer run --console=plain \
  -PdemoText=03/04/2026 -PdemoDateOrder=DMY
```

| Gradle property | Application option | Default / accepted values |
| --- | --- | --- |
| `demoText` | `--text` | Text to parse; omit all options to run the example suite |
| `demoDateOrder` | `--date-order` | `NONE` (default), `MDY`, `DMY`, or `YMD` |
| `demoReference` | `--reference` | `2026-07-03T10:15:00Z`; an ISO instant with an offset |
| `demoTimeZone` | `--time-zone` | `UTC`; a timezone ID such as `Europe/Berlin` |

`NONE` calls the parser without a numeric date-order override. An ambiguous slash date such as `03/04/2026` then produces no match. Set the order explicitly when you know the user's convention. Custom options require `demoText` / `--text`; they do not change the fixed example suite.

The same options can be passed directly to the application. Use either Gradle properties or `--args` for a run:

```sh
./gradlew -p integration/jvm-consumer run --args='--help' --console=plain
./gradlew -p integration/jvm-consumer run \
  --args='--text "Buy milk tomorrow at 7pm" --time-zone UTC' --console=plain
```

The application returns **0** for parsed input or a completed example suite, **1** for no match or an unexpected example result, and **2** for invalid arguments. Invalid date order, reference, and timezone values produce an actionable error. Under `gradlew run`, a nonzero application result appears as a failed Gradle task. Recurrence is a successful parse of metadata; it does not mean a reminder was scheduled.

## Verify the consumer

```sh
./gradlew -p integration/jvm-consumer test --console=plain
```

The tests exercise the actual published parser through this application's entry point: fixed references, English/Russian input, date order, range boundaries, timezone conversion, recurrence, no-match behavior, help, and invalid options. Dependency resolution for the application and tests uses only Maven Central; no `centralOnly` flag is needed.

This example is covered by the repository's [MIT license and author attribution](../../LICENSE).
