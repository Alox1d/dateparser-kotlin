# Changelog

## Unreleased

## 0.1.4 - 2026-11-14

- Reject abbreviated slash-date years such as `13/04/26` instead of interpreting them as AD 26. Four-digit years and supported dates without a year remain available; no century is guessed.
- Reject out-of-range explicit English colon clocks such as `tomorrow at 25:00` and `tomorrow at 19:61` instead of falling back to a partial date match.
- Validate complete cue-attached colon-clock tokens, including overlong fields, trailing colons and extra sections. Valid clocks and the existing `24:30` rollover remain supported.
- Add shared regressions for these cases. Public API signatures and platform requirements are unchanged; unsupported temporal qualifiers still require caller review.
- Expand the runnable examples with a standalone JVM consumer, a SwiftUI host and a reviewed Android calendar handoff, plus updated demo media and CI coverage.

## 0.1.3

- Reject unresolved bare hours in expressions such as `tomorrow at 7`, `Buy milk tomorrow 7`, `2026-10-18 at 7`, `tomorrow at 25`, and `tomorrow 7, please` instead of silently inheriting the reference clock; explicit `7pm` and `19:00` still work.
- Keep emoji-adjacent unsupported hours consistent between JVM and Kotlin/Native.
- Add a runnable Compose reminder editor and confirmation list, using the same shared KMP reminder draft as the independent consumer.
- Add verified Android emulator screenshots, document partial matches and DST behavior in English/Russian, and clarify Android API 24 support requirements. The public API and Android API 26 minimum remain unchanged.

## 0.1.2

- Add `the first Monday of next month` for every full English weekday name, with optional `the` and `first`/`1st`. Resolve from the first day of the next calendar month in the reference timezone.
- Preserve the reference clock and reminder title, with explicit AM/PM support; prevent month names such as May in titles from capturing the generated date.
- Reject recognized unsupported ordinal and recurring variants of this form instead of returning partial weekday/month matches.
- Add eight shared regression tests for weekday coverage, month/year boundaries, timezone-local month selection, clocks, titles and existing `next Friday` behavior. Add a published-artifact consumer regression.
- Update English/Russian documentation, the KMP walkthrough and runnable consumer to 0.1.2. Public API and platform requirements remain unchanged.

## 0.1.1

- Add English named dates in US and European order, month abbreviations, ordinals and four-digit years; ISO and slash dates with explicit `DateOrder` for ambiguous inputs.
- Recognize attached/spaced AM/PM, `today`, `yesterday`, bare weekdays and `next week/month/year`. Explicit AM/PM clocks now zero omitted minutes and seconds.
- Keep compound duration and recurrence expressions together while preserving independent expressions and reminder words.
- Fix range end calendar inheritance, overnight clocks and DST boundary consistency. Reject inverted explicit ranges and unsupported relative range ends.
- Fix reminder text around separators and punctuation on numeric tokens; scope relative offsets to their owning dates.
- Reject non-positive recurrence and add shared regression tests plus separate JVM, Android and iOS published-artifact consumer checks.
- Preserve existing `DateParser.parse`, `ParseOptions` and `DateMatch` signatures. Android API26+, JVM Java8+ and KMP iOS targets remain supported.

## 0.1.0

- Initial Maven Central release with offline English/Russian parsing, Kotlin Multiplatform targets, immutable results, calendar recurrence and bilingual documentation.
