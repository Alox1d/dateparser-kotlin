package io.github.alox1d.dateparser.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class ReminderPreviewParserTest {
    private val parser = ReminderPreviewParser()
    private val reference = Instant.parse("2026-07-03T10:15:00Z").toEpochMilliseconds()

    @Test fun exposesBothRangeBoundariesAndTheEditedTitleSourceToSwift() {
        val preview = assertNotNull(parser.parse(
            "Buy milk, tomorrow from 5PM to 8PM", reference, "Europe/Berlin", "NONE"
        ))
        assertEquals("Buy milk", preview.title)
        assertEquals(epoch("2026-07-04T15:00:00Z"), preview.startEpochMilliseconds)
        assertEquals(epoch("2026-07-04T18:00:00Z"), preview.endEpochMilliseconds)
        assertEquals("Europe/Berlin", preview.timeZoneId)
    }

    @Test fun russianInputHasTheSameExplicitReferenceAsEnglish() {
        val preview = assertNotNull(parser.parse("купить через 5 часов", reference, "UTC", "NONE"))
        assertEquals("купить", preview.title)
        assertEquals(epoch("2026-07-03T15:15:00Z"), preview.startEpochMilliseconds)
        assertNull(preview.endEpochMilliseconds)
    }

    @Test fun ambiguousNumericInputNeedsTheVisibleDateOrderChoice() {
        assertNull(parser.parse("03/04/2026", reference, "UTC", "NONE"))
        val us = assertNotNull(parser.parse("03/04/2026", reference, "UTC", "MDY"))
        val europe = assertNotNull(parser.parse("03/04/2026", reference, "UTC", "DMY"))
        assertEquals(epoch("2026-03-04T10:15:00Z"), us.startEpochMilliseconds)
        assertEquals(epoch("2026-04-03T10:15:00Z"), europe.startEpochMilliseconds)
    }

    @Test fun invalidInputAndRecurringIntervalsStayInManualReview() {
        for (text in listOf("", "Buy milk", "tomorrow at 7", "every 2 days")) {
            assertNull(parser.parse(text, reference, "UTC", "NONE"), text)
        }
    }

    @Test fun invalidBridgeOptionsDoNotThrowAcrossTheSwiftBoundary() {
        assertNull(parser.parse("tomorrow", reference, "Invalid/Timezone", "NONE"))
        assertNull(parser.parse("tomorrow", reference, "UTC", "INVALID"))
    }

    @Test fun aDateOnlyInputLeavesTheTitleForUserEntry() {
        val preview = assertNotNull(parser.parse("tomorrow", reference, "UTC", "NONE"))
        assertEquals("", preview.title)
        assertEquals(epoch("2026-07-04T10:15:00Z"), preview.startEpochMilliseconds)
    }

    private fun epoch(value: String) = Instant.parse(value).toEpochMilliseconds()
}
