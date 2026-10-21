package com.example.dateparser

import android.content.Intent
import android.provider.CalendarContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alox1d.dateparser.sample.parseReminderDraft
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarIntentTest {
    @Test fun parsedBerlinRangeSuppliesBothBoundariesToTheCalendarEditor() {
        val draft = requireNotNull(parseReminderDraft(
            "Work tomorrow from 5PM to 8PM",
            Instant.parse("2026-07-03T10:15:00Z"),
            TimeZone.of("Europe/Berlin")
        ))
        val event = requireNotNull(calendarEventDetails(
            draft.title, draft.startsAt, draft.endsAt, draft.timeZone.id
        ))

        assertCalendarInsert(
            calendarInsertIntent(event),
            title = "Work",
            start = Instant.parse("2026-07-04T15:00:00Z"),
            end = Instant.parse("2026-07-04T18:00:00Z"),
            timeZoneId = "Europe/Berlin"
        )
    }

    @Test fun editedTitleAndOvernightEndReplaceTheOriginalParsedDetails() {
        val draft = requireNotNull(parseReminderDraft(
            "Work tomorrow from 5PM to 8PM",
            Instant.parse("2026-07-03T10:15:00Z"),
            TimeZone.of("Europe/Berlin")
        ))
        val zone = ZoneId.of(draft.timeZone.id)
        val editedStart = resolveReminderTime(
            LocalDateTime.parse("2026-07-04T23:30:00"), draft.startsAt, true, zone
        )
        val editedEnd = resolveReminderTime(
            LocalDateTime.parse("2026-07-05T01:00:00"), draft.endsAt, true, zone
        )
        val event = requireNotNull(calendarEventDetails(
            "  Night shift  ", editedStart, editedEnd, draft.timeZone.id
        ))

        assertCalendarInsert(
            calendarInsertIntent(event),
            title = "Night shift",
            start = Instant.parse("2026-07-04T21:30:00Z"),
            end = Instant.parse("2026-07-04T23:00:00Z"),
            timeZoneId = "Europe/Berlin"
        )
    }

    private fun assertCalendarInsert(
        intent: Intent,
        title: String,
        start: Instant,
        end: Instant,
        timeZoneId: String
    ) {
        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, intent.data)
        assertEquals(title, intent.getStringExtra(CalendarContract.Events.TITLE))
        assertEquals(
            start.toEpochMilliseconds(),
            intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, Long.MIN_VALUE)
        )
        assertEquals(
            end.toEpochMilliseconds(),
            intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, Long.MIN_VALUE)
        )
        assertEquals(timeZoneId, intent.getStringExtra(CalendarContract.Events.EVENT_TIMEZONE))
        assertTrue(intent.hasExtra(CalendarContract.EXTRA_EVENT_ALL_DAY))
        assertFalse(intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true))
    }
}
