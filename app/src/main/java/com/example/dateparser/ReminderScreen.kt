package com.example.dateparser

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

private data class SavedReminder(val title: String, val startsAt: Instant)

private val dayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

@Composable
internal fun ReminderScreen() {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val zone = remember { TimeZone.currentSystemDefault() }
    val zoneId = remember { ZoneId.of(zone.id) }
    var input by remember { mutableStateOf("Buy milk tomorrow at 7pm") }
    var title by remember { mutableStateOf("") }
    var chosenTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var parsedInstant by remember { mutableStateOf<Instant?>(null) }
    var edited by remember { mutableStateOf(false) }
    var previewVisible by remember { mutableStateOf(false) }
    var explanation by remember { mutableStateOf("") }
    val saved = remember { mutableStateListOf<SavedReminder>() }

    fun preview(text: String = input) {
        input = text
        focus.clearFocus()
        keyboard?.hide()
        val result = parseReminderDraft(text, Clock.System.now(), zone)
        previewVisible = true
        edited = false
        parsedInstant = null
        if (result == null || result.endsAt != null) {
            title = text.trim()
            chosenTime = null
            explanation = if (result?.endsAt != null) {
                "This demo saves one time. Enter a single time or choose it below."
            } else {
                "A little more detail? Try 7pm, 19:00, or choose a date and time below."
            }
        } else {
            title = result.title
            parsedInstant = result.startsAt
            chosenTime = LocalDateTime.ofInstant(
                java.time.Instant.parse(result.startsAt.toString()), zoneId
            )
            explanation = "Review the title and time, then make it yours."
        }
    }

    val resolved: Instant? = chosenTime?.let { local ->
        if (!edited) parsedInstant
        else zoneId.rules.getValidOffsets(local).singleOrNull()?.let { offset ->
            Instant.parse(local.toInstant(offset).toString())
        }
    }

    LazyColumn(
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
                        onValueChange = { input = it; previewVisible = false },
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
                            value = title, onValueChange = { title = it },
                            label = { Text("Reminder title") },
                            modifier = Modifier.fillMaxWidth().testTag("reminder_title"),
                            shape = RoundedCornerShape(16.dp), singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TimeTile("Edit date", chosenTime?.format(dayFormat) ?: "Choose date", Modifier.weight(1f)) {
                                focus.clearFocus()
                                val initial = chosenTime ?: LocalDateTime.now(zoneId).plusDays(1)
                                DatePickerDialog(context, { _, y, month, day ->
                                    chosenTime = LocalDateTime.of(y, month + 1, day, initial.hour, initial.minute)
                                    edited = true
                                }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                            }
                            TimeTile("Edit time", chosenTime?.format(timeFormat) ?: "Choose time", Modifier.weight(1f), chosenTime != null) {
                                focus.clearFocus()
                                val initial = chosenTime ?: return@TimeTile
                                TimePickerDialog(context, { _, hour, minute ->
                                    chosenTime = initial.withHour(hour).withMinute(minute)
                                    edited = true
                                }, initial.hour, initial.minute, true).show()
                            }
                        }
                        Text("Timezone: " + zone.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (chosenTime != null && resolved == null) {
                            Text("Selected local time is invalid or overlaps a DST transition.", color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            enabled = title.isNotBlank() && resolved != null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_reminder"),
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                val instant = resolved ?: return@Button
                                focus.clearFocus()
                                keyboard?.hide()
                                saved.add(0, SavedReminder(title.trim(), instant))
                                previewVisible = false
                                input = ""
                            }
                        ) { Text("Add reminder", fontWeight = FontWeight.SemiBold) }
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
                        Text(zone.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Text("DateParser Kotlin demo · No notifications or permanent storage", modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
