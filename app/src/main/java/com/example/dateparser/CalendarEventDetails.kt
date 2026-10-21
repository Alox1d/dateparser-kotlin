package com.example.dateparser

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Instant

internal data class CalendarEventDetails(
    val title: String,
    val beginTimeMillis: Long,
    val endTimeMillis: Long,
    val timeZoneId: String
)

// Calendar export requires a reviewed end. A single instant has no default duration.
internal fun calendarEventDetails(
    title: String,
    startsAt: Instant?,
    endsAt: Instant?,
    timeZoneId: String
): CalendarEventDetails? {
    if (title.isBlank() || startsAt == null || endsAt == null || endsAt <= startsAt) return null
    return CalendarEventDetails(title.trim(), startsAt.toEpochMilliseconds(), endsAt.toEpochMilliseconds(), timeZoneId)
}

// Keep the parser's exact instant until that endpoint is edited. Manual local times
// must have one valid UTC offset, so neither DST gaps nor overlaps are guessed.
internal fun resolveReminderTime(
    chosenTime: LocalDateTime?,
    parsedInstant: Instant?,
    edited: Boolean,
    zoneId: ZoneId
): Instant? {
    if (chosenTime == null) return null
    if (!edited) return parsedInstant
    val offset = zoneId.rules.getValidOffsets(chosenTime).singleOrNull() ?: return null
    return Instant.parse(chosenTime.toInstant(offset).toString())
}
