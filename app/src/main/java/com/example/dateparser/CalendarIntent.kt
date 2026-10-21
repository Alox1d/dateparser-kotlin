package com.example.dateparser

import android.content.Intent
import android.provider.CalendarContract

internal fun calendarInsertIntent(event: CalendarEventDetails): Intent =
    Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, event.title)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.beginTimeMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTimeMillis)
        putExtra(CalendarContract.Events.EVENT_TIMEZONE, event.timeZoneId)
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
    }
