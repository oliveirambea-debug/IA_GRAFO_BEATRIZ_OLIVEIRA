package com.beatriz.tvlauncher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Current local time, refreshed on every minute boundary. */
@Composable
fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50)
        }
    }
    return now
}

@Composable
fun timeText(now: LocalDateTime): String {
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    return now.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm"))
}

@Composable
fun dateText(now: LocalDateTime): String {
    val locale = Locale.getDefault()
    val pattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM") }
    return now.format(DateTimeFormatter.ofPattern(pattern, locale)).replaceFirstChar { it.titlecase(locale) }
}

@Composable
fun ClockBadge(modifier: Modifier = Modifier) {
    val now = rememberNow()
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(timeText(now), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(dateText(now), fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
    }
}
