package com.beatriz.tvlauncher.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.beatriz.tvlauncher.BuildConfig
import com.beatriz.tvlauncher.LauncherViewModel
import com.beatriz.tvlauncher.R
import com.beatriz.tvlauncher.Screen
import kotlinx.coroutines.delay

private val AMBIENT_CHOICES = listOf(1, 3, 5, 10, 20, 0)

private data class EditField(val title: String, val initial: String, val onSave: (String) -> Unit)

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    var editing by remember { mutableStateOf<EditField?>(null) }
    BackHandler { vm.screen = Screen.Home }

    @Suppress("UNUSED_VARIABLE") val version = vm.settingsVersion // re-read prefs after edits
    val prefs = vm.prefs

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF17262F), Color(0xFF080B0E))))
    ) {
        Column(
            Modifier
                .padding(horizontal = 56.dp)
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))

            val cityTitle = stringResource(R.string.setting_city)
            SettingRow(cityTitle, prefs.city.ifBlank { stringResource(R.string.setting_city_unset) }, Modifier.focusRequester(first)) {
                editing = EditField(cityTitle, prefs.city) { vm.updateSettings(city = it) }
            }
            val regionTitle = stringResource(R.string.setting_region)
            SettingRow(regionTitle, prefs.region) {
                editing = EditField(regionTitle, prefs.region) { vm.updateSettings(region = it) }
            }
            val tmdbTitle = stringResource(R.string.setting_tmdb)
            SettingRow(
                tmdbTitle,
                if (prefs.tmdbKey.isBlank()) stringResource(R.string.setting_tmdb_unset)
                else stringResource(R.string.setting_tmdb_set) + " (…" + prefs.tmdbKey.takeLast(4) + ")",
            ) {
                editing = EditField(tmdbTitle, prefs.tmdbKeyOverride) { vm.updateSettings(tmdbKey = it) }
            }
            val minutes = prefs.ambientMinutes
            SettingRow(
                stringResource(R.string.setting_ambient),
                if (minutes == 0) stringResource(R.string.ambient_off) else stringResource(R.string.ambient_minutes, minutes),
            ) {
                val i = AMBIENT_CHOICES.indexOf(minutes)
                vm.updateSettings(ambientMinutes = AMBIENT_CHOICES[(i + 1) % AMBIENT_CHOICES.size])
            }
            SettingRow(stringResource(R.string.show_ambient), null) {
                vm.screen = Screen.Home
                vm.ambient = true
            }
            SettingRow(stringResource(R.string.android_settings), null) {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.about, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }

    editing?.let { field ->
        TextEntryDialog(field, onDismiss = { editing = null })
    }

    LaunchedEffect(Unit) {
        delay(60)
        runCatching { first.requestFocus() }
    }
}

@Composable
private fun SettingRow(title: String, value: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.07f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (value != null) {
                Spacer(Modifier.width(16.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TextEntryDialog(field: EditField, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(field.initial) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val save = {
        field.onSave(text)
        onDismiss()
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1F26))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(field.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { MenuRow(stringResource(R.string.save)) { save() } }
                Box(Modifier.weight(1f)) { MenuRow(stringResource(R.string.cancel)) { onDismiss() } }
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }
}
