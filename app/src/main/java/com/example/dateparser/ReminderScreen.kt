package com.example.dateparser

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alox1d.dateparser.sample.parseReminderDraft
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

private data class SavedReminder(val title: String, val startsAt: Instant, val endsAt: Instant?)

private val dayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

@Composable
internal fun ReminderScreen() {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val zone = remember { TimeZone.currentSystemDefault() }
    val zoneId = remember { ZoneId.of(zone.id) }
    var input by remember { mutableStateOf("Buy milk tomorrow at 7pm") }
    var title by remember { mutableStateOf("") }
    var chosenTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var chosenEndTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var parsedInstant by remember { mutableStateOf<Instant?>(null) }
    var parsedEndInstant by remember { mutableStateOf<Instant?>(null) }
    var edited by remember { mutableStateOf(false) }
    var endEdited by remember { mutableStateOf(false) }
    var previewVisible by remember { mutableStateOf(false) }
    var explanation by remember { mutableStateOf("") }
    var calendarMessage by remember { mutableStateOf("") }
    val saved = remember { mutableStateListOf<SavedReminder>() }

    fun preview(text: String = input) {
        input = text
        focus.clearFocus()
        keyboard?.hide()
        val result = parseReminderDraft(text, Clock.System.now(), zone)
        previewVisible = true
        edited = false
        endEdited = false
        parsedInstant = null
        parsedEndInstant = null
        chosenEndTime = null
        calendarMessage = ""
        if (result == null) {
            title = text.trim()
            chosenTime = null
            explanation = "A little more detail? Try 7pm, 19:00, or choose a date and time below."
        } else {
            title = result.title
            parsedInstant = result.startsAt
            parsedEndInstant = result.endsAt
            chosenTime = LocalDateTime.ofInstant(
                java.time.Instant.parse(result.startsAt.toString()), zoneId
            )
            chosenEndTime = result.endsAt?.let { end ->
                LocalDateTime.ofInstant(java.time.Instant.parse(end.toString()), zoneId)
            }
            explanation = if (result.endsAt != null) {
                "This phrase includes a range. Review both dates and times; both will be kept."
            } else {
                "Review the title and time, then make it yours."
            }
        }
    }

    val resolved = resolveReminderTime(chosenTime, parsedInstant, edited, zoneId)
    val resolvedEnd = resolveReminderTime(chosenEndTime, parsedEndInstant, endEdited, zoneId)
    val validEnd = chosenEndTime == null || (resolved != null && resolvedEnd != null && resolvedEnd > resolved)
    val calendarEvent = calendarEventDetails(title, resolved, resolvedEnd, zone.id)

    fun chooseDuration(durationMinutes: Int) {
        val start = resolved ?: return
        val end = start + durationMinutes.minutes
        parsedEndInstant = end
        chosenEndTime = LocalDateTime.ofInstant(java.time.Instant.parse(end.toString()), zoneId)
        endEdited = false
        calendarMessage = ""
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding().imePadding()
            .semantics { testTagsAsResourceId = true },
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ReminderHero() }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Eyebrow("PUT IT INTO WORDS")
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it; previewVisible = false; calendarMessage = "" },
                        label = { Text("Describe your reminder") },
                        placeholder = { Text("Buy milk tomorrow at 7pm") },
                        modifier = Modifier.fillMaxWidth().testTag("reminder_input"),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 1, maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { preview() })
                    )
                    Button(
                        onClick = { preview() }, enabled = input.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("preview_reminder"),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Preview reminder  →", fontWeight = FontWeight.SemiBold) }
                    if (!previewVisible) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(onClick = { preview("Buy milk tomorrow at 7pm") }, label = { Text("Tomorrow, 7pm") })
                            SuggestionChip(onClick = { preview("Take a break in 2 hours") }, label = { Text("In 2 hours") })
                            SuggestionChip(onClick = { preview("Купить молоко завтра в 19:00") }, label = { Text("По-русски") })
                        }
                    }
                }
            }
        }
        if (previewVisible) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("reminder_preview"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatusPill(
                            if (chosenTime == null) "NEEDS A LITTLE CLARITY" else "READY FOR YOUR REVIEW",
                            if (chosenTime == null) Color(0xFFFFE9C5) else Color(0xFFDFF0CE)
                        )
                        Text("Check before saving", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = title, onValueChange = { title = it; calendarMessage = "" },
                            label = { Text("Reminder title") },
                            modifier = Modifier.fillMaxWidth().testTag("reminder_title"),
                            shape = RoundedCornerShape(16.dp), singleLine = true
                        )
                        ReminderTimeEditor("Start", chosenTime, zoneId) {
                            chosenTime = it
                            edited = true
                            calendarMessage = ""
                        }
                        if (chosenEndTime != null) {
                            ReminderTimeEditor("End", chosenEndTime, zoneId) {
                                chosenEndTime = it
                                endEdited = true
                                calendarMessage = ""
                            }
                        }
                        Text("Timezone: " + zone.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (chosenTime != null && resolved == null) {
                            Text("Selected start time is invalid or overlaps a DST transition.", color = MaterialTheme.colorScheme.error)
                        }
                        if (chosenEndTime != null && resolvedEnd == null) {
                            Text("Selected end time is invalid or overlaps a DST transition.", color = MaterialTheme.colorScheme.error)
                        } else if (resolved != null && resolvedEnd != null && resolvedEnd <= resolved) {
                            Text("Choose an end after the start. For an overnight event, change the end date too.", color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            enabled = title.isNotBlank() && resolved != null && validEnd,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_reminder"),
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                val instant = resolved ?: return@Button
                                focus.clearFocus()
                                keyboard?.hide()
                                saved.add(0, SavedReminder(title.trim(), instant, resolvedEnd))
                                previewVisible = false
                                input = ""
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        ) { Text("Add reminder", fontWeight = FontWeight.SemiBold) }
                        Text("Add reminder keeps these details for this session only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Eyebrow("SAVE IN YOUR CALENDAR")
                        if (chosenEndTime == null) {
                            Text("Calendar events need an end. Choose a duration, then adjust the end date or time above.", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(
                                    onClick = { chooseDuration(30) }, enabled = resolved != null,
                                    label = { Text("30 minutes") }, modifier = Modifier.testTag("duration_30_minutes")
                                )
                                SuggestionChip(
                                    onClick = { chooseDuration(60) }, enabled = resolved != null,
                                    label = { Text("1 hour") }, modifier = Modifier.testTag("duration_1_hour")
                                )
                            }
                        } else if (resolved != null && resolvedEnd != null && validEnd) {
                            Text("Calendar duration: ${resolvedEnd - resolved}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Open an editable event with the reviewed title, start and end. Save or cancel in your calendar app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(
                            enabled = calendarEvent != null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_calendar"),
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                val event = calendarEvent ?: return@OutlinedButton
                                focus.clearFocus()
                                keyboard?.hide()
                                val intent = calendarInsertIntent(event)
                                calendarMessage = try {
                                    context.startActivity(intent)
                                    "Calendar editor opened. Save or cancel the event there."
                                } catch (_: ActivityNotFoundException) {
                                    "No calendar app can open this event. Your reviewed details are still here."
                                }
                            }
                        ) { Text("Open in calendar", fontWeight = FontWeight.SemiBold) }
                        if (calendarMessage.isNotEmpty()) {
                            Text(calendarMessage, modifier = Modifier.testTag("calendar_status"), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Your reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                StatusPill(saved.size.toString(), MaterialTheme.colorScheme.primaryContainer)
            }
        }
        if (saved.isEmpty()) {
            item {
                OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("A sentence now. A plan for later.", fontFamily = FontFamily.Serif, fontSize = 21.sp)
                        Text("Confirmed reminders appear here. Kept for this session only.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(saved) { reminder ->
            val local = LocalDateTime.ofInstant(java.time.Instant.parse(reminder.startsAt.toString()), zoneId)
            val localEnd = reminder.endsAt?.let { end ->
                LocalDateTime.ofInstant(java.time.Instant.parse(end.toString()), zoneId)
            }
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.widthIn(min = 60.dp).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(local.format(monthFormat).uppercase(Locale.ENGLISH), style = MaterialTheme.typography.labelSmall)
                            Text(local.dayOfMonth.toString(), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(local.format(dayFormat) + " · " + local.format(timeFormat), style = MaterialTheme.typography.bodyMedium)
                        if (localEnd != null) {
                            Text("Until " + localEnd.format(dayFormat) + " · " + localEnd.format(timeFormat), style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(zone.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Text("DateParser Kotlin demo · Session reminders stay here. Save calendar events in your calendar app. No notifications are scheduled by this demo.", modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReminderTimeEditor(label: String, chosenTime: LocalDateTime?, zoneId: ZoneId, onChange: (LocalDateTime) -> Unit) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val tag = label.lowercase(Locale.ROOT)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TimeTile("$label date", chosenTime?.format(dayFormat) ?: "Choose date", Modifier.weight(1f).testTag("${tag}_date")) {
            focus.clearFocus()
            val initial = chosenTime ?: LocalDateTime.now(zoneId).plusDays(1)
            DatePickerDialog(context, { _, y, month, day ->
                onChange(LocalDateTime.of(y, month + 1, day, initial.hour, initial.minute))
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
        }
        TimeTile("$label time", chosenTime?.format(timeFormat) ?: "Choose time", Modifier.weight(1f).testTag("${tag}_time"), chosenTime != null) {
            focus.clearFocus()
            val initial = chosenTime ?: return@TimeTile
            TimePickerDialog(context, { _, hour, minute ->
                onChange(initial.withHour(hour).withMinute(minute).withSecond(0).withNano(0))
            }, initial.hour, initial.minute, true).show()
        }
    }
}

@Composable
private fun ReminderHero() {
    Row(
        Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(Color(0xFF432076), Color(0xFF7928CA))), RoundedCornerShape(28.dp)
        ).padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DATEPARSER KOTLIN", color = Color(0xFFE6D4FF), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
            Text("Make time\nfor it.", color = Color.White, fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 36.sp)
            Text("OFFLINE  ·  EN + RU", color = Color(0xFFDFF0CE), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Canvas(Modifier.size(86.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(Color.White.copy(alpha = 0.08f))
            drawCircle(Color(0xFFE6D4FF), radius = size.width * .36f, style = Stroke(2.dp.toPx()))
            drawLine(Color.White, center, Offset(center.x, size.height * .27f), 3.dp.toPx(), StrokeCap.Round)
            drawLine(Color.White, center, Offset(size.width * .32f, size.height * .62f), 3.dp.toPx(), StrokeCap.Round)
            drawCircle(Color(0xFFDFF0CE), radius = 5.dp.toPx(), center = center)
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimeTile(label: String, value: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(12.dp)
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
