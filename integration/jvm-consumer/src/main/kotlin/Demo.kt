package io.github.alox1d.dateparser.jvmconsumer

import io.github.alox1d.dateparser.DateOrder
import io.github.alox1d.dateparser.DateParser
import kotlin.system.exitProcess
import kotlin.time.Instant
import kotlinx.datetime.DateTimeArithmeticException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val DEFAULT_REFERENCE = "2026-07-03T10:15:00Z"
private const val FOOTER = "Preview only. No reminder is saved or scheduled."

fun main(args: Array<String>) {
    exitProcess(runDemo(args, System.out, System.err))
}

internal fun runDemo(args: Array<String>, out: Appendable, err: Appendable): Int {
    if (args.contentEquals(arrayOf("--help"))) {
        out.appendLine(HELP)
        return 0
    }
    if (args.isEmpty()) return runExamples(out, err)

    val input = try {
        parseArguments(args)
    } catch (error: IllegalArgumentException) {
        err.appendLine("ERROR: ${error.message}")
        err.appendLine("Use --help for options and examples.")
        return 2
    }

    out.appendLine("DateParser Kotlin 0.1.3 | JVM consumer")
    val matched = showResult(input, out)
    out.appendLine(FOOTER)
    if (!matched) {
        err.appendLine("Review the input: choose --date-order for a slash date, use an explicit time such as 7pm or 19:00, or enter a supported date manually.")
    }
    return if (matched) 0 else 1
}

private data class Input(
    val text: String,
    val reference: Instant = Instant.parse(DEFAULT_REFERENCE),
    val timeZone: TimeZone = TimeZone.UTC,
    val dateOrder: DateOrder? = null
)

private fun parseArguments(args: Array<String>): Input {
    val allowed = setOf("--text", "--reference", "--time-zone", "--date-order")
    val values = mutableMapOf<String, String>()
    var index = 0
    while (index < args.size) {
        val option = args[index]
        require(option in allowed) { "Unknown option '$option'." }
        require(index + 1 < args.size && !args[index + 1].startsWith("--")) {
            "Missing value for $option."
        }
        require(option !in values) { "Duplicate option $option." }
        values[option] = args[index + 1]
        index += 2
    }
    val text = requireNotNull(values["--text"]) {
        "--text is required when providing options; run without arguments for the fixed example suite."
    }

    val rawOrder = values["--date-order"] ?: "NONE"
    val dateOrder = if (rawOrder == "NONE") null else try {
        DateOrder.valueOf(rawOrder)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid --date-order '$rawOrder'; use NONE, MDY, DMY, or YMD.")
    }

    val rawReference = values["--reference"] ?: DEFAULT_REFERENCE
    val reference = try {
        Instant.parse(rawReference)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid --reference '$rawReference'; use an ISO instant such as $DEFAULT_REFERENCE.")
    }

    val rawZone = values["--time-zone"] ?: "UTC"
    val timeZone = try {
        TimeZone.of(rawZone)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid --time-zone '$rawZone'; use UTC or a timezone ID such as Europe/Berlin.")
    }
    try {
        reference.toLocalDateTime(timeZone)
    } catch (_: DateTimeArithmeticException) {
        throw IllegalArgumentException("Invalid --reference '$rawReference' for --time-zone '$rawZone'; outside the supported calendar range.")
    }
    return Input(text, reference, timeZone, dateOrder)
}

private fun showResult(input: Input, out: Appendable): Boolean {
    out.appendLine("Input: ${input.text}")
    out.appendLine("Reference: ${input.reference}")
    out.appendLine("Time zone: ${input.timeZone.id} | Date order: ${input.dateOrder?.name ?: "NONE"}")

    val parser = DateParser()
    // Always pass the reference and timezone explicitly, including when no date order is chosen.
    val matches = input.dateOrder?.let { order ->
        parser.parse(input.text, order, input.reference, input.timeZone)
    } ?: parser.parse(input.text, input.reference, input.timeZone)
    if (matches.isEmpty()) {
        out.appendLine("NO MATCH: no supported, unambiguous date/time was recognized.")
        return false
    }

    for ((index, match) in matches.withIndex()) {
        out.appendLine("MATCH ${index + 1}/${matches.size}")
        out.appendLine("Remaining text: ${match.remainingText.ifBlank { "(empty)" }}")
        out.appendLine("Start (local): ${match.targetDate}")
        out.appendLine("Start (UTC): ${match.instant}")
        match.endDate?.let { out.appendLine("End (local): $it") }
        match.endInstant?.let { out.appendLine("End (UTC): $it") }
        match.recurrence?.let {
            out.appendLine("Recurrence: $it (calendar interval; metadata only)")
            out.appendLine("Your application must define the recurrence anchor and scheduling policy.")
        }
    }
    return true
}

private data class Example(val input: Input, val expectsMatch: Boolean = true)

private fun runExamples(out: Appendable, err: Appendable): Int {
    val examples = listOf(
        Example(Input("Buy milk, tomorrow from 5PM to 8PM")),
        Example(Input("купить через 5 часов")),
        Example(Input("Meet on July 4, 2026 at 5PM", timeZone = TimeZone.of("Europe/Berlin"))),
        Example(Input("Meet on 4th July 2026 at 5 pm", timeZone = TimeZone.of("America/New_York"))),
        Example(Input("03/04/2026", dateOrder = DateOrder.MDY)),
        Example(Input("03/04/2026", dateOrder = DateOrder.DMY)),
        Example(Input("03/04/2026"), expectsMatch = false),
        Example(Input("Buy milk tomorrow at 7"), expectsMatch = false),
        Example(Input("July 32, 2026"), expectsMatch = false),
        Example(Input("every 2 days"))
    )

    out.appendLine("DateParser Kotlin 0.1.3 | JVM consumer | fixed examples")
    var unexpectedResults = 0
    for (example in examples) {
        out.appendLine()
        out.appendLine("Expected: ${if (example.expectsMatch) "MATCH" else "NO MATCH"}")
        val matched = showResult(example.input, out)
        if (matched != example.expectsMatch) {
            err.appendLine("Unexpected parser result for '${example.input.text}'.")
            unexpectedResults++
        }
    }
    out.appendLine()
    out.appendLine(FOOTER)
    if (unexpectedResults == 0) out.appendLine("All ${examples.size} examples behaved as described.")
    return if (unexpectedResults == 0) 0 else 1
}

private val HELP = """
    DateParser Kotlin 0.1.3 | JVM consumer
    Run without arguments for the fixed example suite, or:
      --text "Buy milk tomorrow at 7pm" [options]

    Options:
      --text TEXT          Text to parse; required for a custom run.
      --date-order ORDER   NONE, MDY, DMY, or YMD (default: NONE).
      --reference INSTANT  ISO instant (default: $DEFAULT_REFERENCE).
      --time-zone ZONE     Timezone ID (default: UTC); for example Europe/Berlin.
      --help               Print this help; use on its own.

    Quote text containing spaces. Dates without a time inherit the reference clock.
    Recurrence is calendar metadata; your application supplies any scheduling policy.
    Exit codes: 0 = parsed input or expected example results; 1 = no match; 2 = invalid arguments.
    $FOOTER
""".trimIndent()
