package com.beatriz.tvlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.beatriz.tvlauncher.LauncherViewModel
import com.beatriz.tvlauncher.Screen

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color(0xFF111318),
            surface = Color(0xFF15171C),
            onSurface = Color.White,
            background = Color.Black,
            onBackground = Color.White,
        ),
        content = content,
    )
}

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (vm.screen) {
            Screen.Home -> HomeScreen(vm)
            Screen.AllApps -> AllAppsScreen(vm)
            Screen.Settings -> SettingsScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.ambient,
            enter = fadeIn(tween(1500)),
            exit = fadeOut(tween(500)),
        ) {
            AmbientScreen(vm.weather)
        }
    }
}
