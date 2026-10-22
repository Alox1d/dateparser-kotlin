package com.example.dateparser

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val reminderColors = lightColorScheme(
    primary = Color(0xFF7928CA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE2FF),
    onPrimaryContainer = Color(0xFF432076),
    background = Color(0xFFF4F6FF),
    onBackground = Color(0xFF292333),
    surface = Color.White,
    onSurface = Color(0xFF292333),
    surfaceVariant = Color(0xFFF0EBF4),
    onSurfaceVariant = Color(0xFF68616F),
    outline = Color(0xFF9A8EAA),
    outlineVariant = Color(0xFFE2D9EB),
    error = Color(0xFF9C4324)
)

@Composable
internal fun ReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = reminderColors, content = content)
}
