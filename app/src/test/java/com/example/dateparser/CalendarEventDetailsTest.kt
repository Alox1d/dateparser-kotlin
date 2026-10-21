package com.example.dateparser

import io.github.alox1d.dateparser.sample.parseReminderDraft
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarEventDetailsTest {
    private val start = Instant.parse("2026-07-04T17:00:00Z")
    private val end = Instant.parse("2026-07-04T20:00:00Z")

    @Test fun keepsBothParsedRangeBoundariesInTheCalendarPayload() {
        val draft = requireNotNull(parseReminderDraft(
            "Work tomorrow from 5PM to 8PM",
            Instant.parse("2026-07-03T10:15:00Z"),
            TimeZone.of("Europe/Berlin")
        ))
        val event = requireNotNull(calendarEventDetails(
            "  Edited work title  ", draft.startsAt, draft.endsAt, draft.timeZone.id
        ))

        assertEquals("Edited work title", event.title)
        assertEquals(Instant.parse("2026-07-04T15:00:00Z").toEpochMilliseconds(), event.beginTimeMillis)
        assertEquals(Instant.parse("2026-07-04T18:00:00Z").toEpochMilliseconds(), event.endTimeMillis)
        assertEquals("Europe/Berlin", event.timeZoneId)
    }

    @Test fun aSingleInstantRequiresAnExplicitEndForCalendarExport() {
        val draft = requireNotNull(parseReminderDraft(
            "Buy milk tomorrow at 7pm",
            Instant.parse("2026-07-03T10:15:00Z"),
            TimeZone.UTC
        ))

        assertNull(calendarEventDetails(draft.title, draft.startsAt, draft.endsAt, draft.timeZone.id))
    }

    @Test fun acceptsAnExplicitEndOnTheFollowingDay() {
        val event = requireNotNull(calendarEventDetails(
            "Night shift", Instant.parse("2026-07-04T23:30:00Z"),
            Instant.parse("2026-07-05T01:00:00Z"), "UTC"
        ))

        assertEquals(5_400_000L, event.endTimeMillis - event.beginTimeMillis)
    }

    @Test fun requiresAnEndStrictlyAfterTheStart() {
        assertNull(calendarEventDetails("Work", start, start, "UTC"))
        assertNull(calendarEventDetails("Work", end, start, "UTC"))
    }

    @Test fun incompleteReviewCannotCreateACalendarPayload() {
        assertNull(calendarEventDetails("  ", start, end, "UTC"))
        assertNull(calendarEventDetails("Work", null, end, "UTC"))
    }

    @Test fun preservesTheParsedInstantAtAnOverlappingLocalTime() {
        val parsed = Instant.parse("2026-10-25T01:30:00Z")
        val resolved = resolveReminderTime(
            LocalDateTime.parse("2026-10-25T02:30:00"), parsed, false,
            ZoneId.of("Europe/Berlin")
        )

        assertEquals(parsed, resolved)
    }

    @Test fun rejectsManuallySelectedTimesInADstGapOrOverlap() {
        val berlin = ZoneId.of("Europe/Berlin")

        assertNull(resolveReminderTime(LocalDateTime.parse("2026-03-29T02:30:00"), start, true, berlin))
        assertNull(resolveReminderTime(LocalDateTime.parse("2026-10-25T02:30:00"), start, true, berlin))
    }

    @Test fun resolvesAManuallyEditedTimeUsingTheDisplayedTimezone() {
        val resolved = resolveReminderTime(
            LocalDateTime.parse("2026-07-04T19:15:00"), start, true,
            ZoneId.of("Europe/Berlin")
        )

        assertEquals(Instant.parse("2026-07-04T17:15:00Z"), resolved)
    }

    @Test fun manualSelectionDoesNotNeedAParsedInstant() {
        val resolved = resolveReminderTime(
            LocalDateTime.parse("2026-07-04T19:15:00"), null, true, ZoneId.of("UTC")
        )

        assertNotNull(resolved)
        assertEquals(Instant.parse("2026-07-04T19:15:00Z"), resolved)
    }
}
