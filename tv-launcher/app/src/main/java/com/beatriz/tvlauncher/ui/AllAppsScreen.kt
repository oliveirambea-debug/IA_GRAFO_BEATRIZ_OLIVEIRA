package com.beatriz.tvlauncher.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.beatriz.tvlauncher.LauncherViewModel
import com.beatriz.tvlauncher.R
import com.beatriz.tvlauncher.Screen
import kotlinx.coroutines.delay

@Composable
fun AllAppsScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    BackHandler { vm.screen = Screen.Home }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1E1B33), Color(0xFF0B0B12))))
    ) {
        Column(Modifier.padding(start = 56.dp, end = 56.dp, top = 40.dp)) {
            Text(stringResource(R.string.all_apps), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(stringResource(R.string.all_apps_hint), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            ) {
                itemsIndexed(vm.apps, key = { _, a -> a.packageName }) { index, app ->
                    val pinned = vm.isPinned(app.packageName)
                    Column {
                        Surface(
                            onClick = { vm.launch(context, app) },
                            onLongClick = {
                                val nowPinned = vm.togglePin(app.packageName)
                                Toast.makeText(
                                    context,
                                    if (nowPinned) R.string.pinned else R.string.unpinned,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .then(if (index == 0) Modifier.focusRequester(first) else Modifier),
                            shape = ClickableSurfaceDefaults.shape(TileShape),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                            border = tileBorder(),
                            colors = tileColors(),
                        ) {
                            AppArt(app)
                            if (pinned) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            app.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(vm.apps.isNotEmpty()) {
        delay(60)
        runCatching { first.requestFocus() }
    }
}
