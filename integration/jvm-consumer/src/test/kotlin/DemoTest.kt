package io.github.alox1d.dateparser.jvmconsumer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoTest {
    @Test fun defaultExamplesUseAFixedReferenceAndLabelExpectedNoMatches() {
        val result = run()

        assertEquals(0, result.exitCode, result.stderr)
        assertEquals("", result.stderr)
        assertTrue("Reference: 2026-07-03T10:15:00Z" in result.stdout)
        assertTrue("купить через 5 часов" in result.stdout)
        assertTrue("Expected: NO MATCH" in result.stdout)
        assertTrue("Recurrence: P2D" in result.stdout)
        assertTrue("No reminder is saved or scheduled." in result.stdout)
    }

    @Test fun parsesEnglishAndRussianUsingTheSuppliedReference() {
        for (text in listOf("buy in 5 hours", "купить через 5 часов")) {
            val result = run("--text", text, "--reference", "2024-02-28T10:15:00Z")

            assertEquals(0, result.exitCode, result.stderr)
            assertTrue("Start (UTC): 2024-02-28T15:15:00Z" in result.stdout, text)
        }
    }

    @Test fun showsBothRangeBoundariesAndTheRemainingReminderText() {
        val result = run("--text", "Buy milk, tomorrow from 5PM to 8PM")

        assertEquals(0, result.exitCode, result.stderr)
        assertTrue("Remaining text: Buy milk" in result.stdout)
        assertTrue("Start (local): 2026-07-04T17:00" in result.stdout)
        assertTrue("Start (UTC): 2026-07-04T17:00:00Z" in result.stdout)
        assertTrue("End (local): 2026-07-04T20:00" in result.stdout)
        assertTrue("End (UTC): 2026-07-04T20:00:00Z" in result.stdout)
    }

    @Test fun resolvesANamedDateInTheRequestedTimeZone() {
        val result = run(
            "--text", "Meet on July 4, 2026 at 5PM",
            "--time-zone", "Europe/Berlin"
        )

        assertEquals(0, result.exitCode, result.stderr)
        assertTrue("Time zone: Europe/Berlin" in result.stdout)
        assertTrue("Start (local): 2026-07-04T17:00" in result.stdout)
        assertTrue("Start (UTC): 2026-07-04T15:00:00Z" in result.stdout)
    }

    @Test fun dateOnlyInputUsesTheReferenceClockAndLocalCalendarDate() {
        val result = run(
            "--text", "tomorrow",
            "--reference", "2026-07-03T23:15:00Z",
            "--time-zone", "Europe/Berlin"
        )

        assertEquals(0, result.exitCode, result.stderr)
        assertTrue("Start (local): 2026-07-05T01:15" in result.stdout)
        assertTrue("Start (UTC): 2026-07-04T23:15:00Z" in result.stdout)
        assertFalse("End (UTC):" in result.stdout)
    }

    @Test fun appliesTheExplicitNumericDateOrderWithoutMovingPastDatesForward() {
        for ((order, expected) in listOf("MDY" to "2026-03-04", "DMY" to "2026-04-03")) {
            val result = run("--text", "03/04/2026", "--date-order", order)

            assertEquals(0, result.exitCode, result.stderr)
            assertTrue("Start (UTC): ${expected}T10:15:00Z" in result.stdout, order)
        }
        val yearFirst = run("--text", "2026/03/04", "--date-order", "YMD")
        assertEquals(0, yearFirst.exitCode, yearFirst.stderr)
        assertTrue("Start (UTC): 2026-03-04T10:15:00Z" in yearFirst.stdout)
    }

    @Test fun ambiguousAndUnsupportedInputReturnsANonzeroStatus() {
        for (text in listOf("", "Buy milk", "03/04/2026", "Buy milk tomorrow at 7", "July 32, 2026")) {
            val result = run("--text", text)

            assertEquals(1, result.exitCode, text)
            assertTrue("NO MATCH" in result.stdout, text)
            assertTrue("--date-order" in result.stderr, text)
            assertFalse("Start (UTC):" in result.stdout, text)
        }
        assertEquals(1, run("--text", "03/04/2026", "--date-order", "NONE").exitCode)
    }

    @Test fun recurrenceRemainsCalendarMetadataAndDoesNotBecomeAScheduledReminder() {
        val result = run("--text", "every 1 month and 2 days")

        assertEquals(0, result.exitCode, result.stderr)
        assertTrue("Recurrence: P1M2D" in result.stdout)
        assertTrue("metadata only" in result.stdout)
        assertTrue("No reminder is saved or scheduled." in result.stdout)
    }

    @Test fun helpExplainsOptionsAndStatusesWithoutParsingAnInput() {
        val result = run("--help")

        assertEquals(0, result.exitCode)
        assertEquals("", result.stderr)
        for (option in listOf("--text", "--date-order", "--reference", "--time-zone")) {
            assertTrue(option in result.stdout)
        }
        assertTrue("1 = no match" in result.stdout)
        assertFalse("Start (UTC):" in result.stdout)
    }

    @Test fun malformedOptionValuesGiveActionableErrors() {
        for ((option, value, hint) in listOf(
            Triple("--date-order", "AUTO", "NONE, MDY, DMY, or YMD"),
            Triple("--reference", "2026-07-03", "2026-07-03T10:15:00Z"),
            Triple("--time-zone", "Not/A_Zone", "Europe/Berlin")
        )) {
            val result = run("--text", "tomorrow", option, value)

            assertEquals(2, result.exitCode, option)
            assertTrue(option in result.stderr, option)
            assertTrue(value in result.stderr, option)
            assertTrue(hint in result.stderr, option)
            assertFalse("Start (UTC):" in result.stdout, option)
        }
    }

    @Test fun missingUnknownAndDuplicateOptionsAreRejected() {
        val cases = listOf(
            arrayOf("--text") to "Missing value for --text",
            arrayOf("--text", "--reference", "2026-07-03T10:15:00Z") to "Missing value for --text",
            arrayOf("--unknown", "value") to "Unknown option '--unknown'",
            arrayOf("tomorrow") to "Unknown option 'tomorrow'",
            arrayOf("--text", "today", "--text", "tomorrow") to "Duplicate option --text",
            arrayOf("--time-zone", "UTC") to "--text is required"
        )
        for ((args, hint) in cases) {
            val result = run(*args)

            assertEquals(2, result.exitCode, args.contentToString())
            assertTrue(hint in result.stderr, result.stderr)
            assertTrue("--help" in result.stderr)
        }
    }

    @Test fun anInstantOutsideTheLocalCalendarRangeIsAnArgumentError() {
        val result = run(
            "--text", "tomorrow", "--reference", "+1000000000-01-01T00:00:00Z", "--time-zone", "UTC"
        )
        assertEquals(2, result.exitCode)
        assertTrue("--reference" in result.stderr)
        assertTrue("supported calendar range" in result.stderr)
        assertFalse("Start (UTC):" in result.stdout)
    }

    private fun run(vararg args: String): Result {
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val exitCode = runDemo(arrayOf(*args), stdout, stderr)
        return Result(exitCode, stdout.toString(), stderr.toString())
    }

    private data class Result(val exitCode: Int, val stdout: String, val stderr: String)
}
