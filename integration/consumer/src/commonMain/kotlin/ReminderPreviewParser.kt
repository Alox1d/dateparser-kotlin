package io.github.alox1d.dateparser.sample

import io.github.alox1d.dateparser.DateOrder
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** Primitive values keep the sample's Swift boundary independent of Kotlin date/time types. */
data class ReminderPreview(
    val title: String,
    val startEpochMilliseconds: Long,
    val endEpochMilliseconds: Long?,
    val timeZoneId: String
)

/** Application adapter for the iOS host, using the same review policy as [parseReminderDraft]. */
class ReminderPreviewParser {
    fun parse(
        text: String,
        referenceEpochMilliseconds: Long,
        timeZoneId: String,
        dateOrder: String
    ): ReminderPreview? {
        val order = when (dateOrder) {
            "NONE" -> null
            "MDY" -> DateOrder.MDY
            "DMY" -> DateOrder.DMY
            "YMD" -> DateOrder.YMD
            else -> return null
        }
        val zone = try {
            TimeZone.of(timeZoneId)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val draft = parseReminderDraft(
            text, Instant.fromEpochMilliseconds(referenceEpochMilliseconds), zone, order
        ) ?: return null
        return ReminderPreview(
            draft.title,
            draft.startsAt.toEpochMilliseconds(),
            draft.endsAt?.toEpochMilliseconds(),
            draft.timeZone.id
        )
    }
}
