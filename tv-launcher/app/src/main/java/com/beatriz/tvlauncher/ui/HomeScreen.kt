package com.beatriz.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.beatriz.tvlauncher.LauncherViewModel
import com.beatriz.tvlauncher.R
import com.beatriz.tvlauncher.Screen
import com.beatriz.tvlauncher.data.AppEntry
import com.beatriz.tvlauncher.data.ProviderFeed
import com.beatriz.tvlauncher.data.Providers
import com.beatriz.tvlauncher.data.Title
import com.beatriz.tvlauncher.data.Tmdb
import kotlinx.coroutines.delay

private val DockHeight: Dp = 150.dp

sealed class DockItem(val key: String) {
    data object AllApps : DockItem("__all_apps")
    data object Settings : DockItem("__settings")
    data class App(val entry: AppEntry) : DockItem(entry.packageName)
}

private data class HeroState(val item: DockItem, val feed: ProviderFeed?)

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val dockApps = vm.dockApps
    val items = remember(dockApps) {
        listOf<DockItem>(DockItem.AllApps) + dockApps.map { DockItem.App(it) } + DockItem.Settings
    }
    val selected = items.firstOrNull { it.key == vm.homeSelected } ?: items.getOrElse(1) { items.first() }
    val requesters = remember { HashMap<String, FocusRequester>() }
    val requester = { key: String -> requesters.getOrPut(key) { FocusRequester() } }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }

    BackHandler(enabled = true) { /* this is home: nothing to go back to */ }

    LaunchedEffect(selected.key) {
        delay(250)
        (selected as? DockItem.App)?.let { vm.requestFeed(it.entry) }
    }
    LaunchedEffect(items.size) {
        delay(60)
        runCatching { requester(selected.key).requestFocus() }
    }

    val openSelected: () -> Unit = {
        when (val s = selected) {
            is DockItem.App -> vm.launch(context, s.entry)
            DockItem.AllApps -> vm.screen = Screen.AllApps
            DockItem.Settings -> vm.screen = Screen.Settings
        }
    }

    val feed = (selected as? DockItem.App)?.let { vm.feedFor(it.entry) }
    val showTmdbHint = selected is DockItem.App && Providers.match(selected.entry) != null && vm.prefs.tmdbKey.isBlank()

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = HeroState(selected, feed),
            transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(450)) },
            contentKey = { it.item.key + ":" + (it.feed?.hero?.id ?: 0) },
            label = "hero",
        ) { state ->
            when (val item = state.item) {
                is DockItem.App ->
                    if (state.feed != null) FeedHero(item.entry, state.feed, openSelected, onTitle = { openSelected() })
                    else AppHero(item.entry, showTmdbHint, openSelected)
                DockItem.AllApps -> SimpleHero(
                    title = stringResource(R.string.all_apps),
                    subtitle = stringResource(R.string.apps_installed, vm.apps.size),
                    colors = listOf(Color(0xFF3B2D6B), Color(0xFF14122A), Color.Black),
                    onOpen = openSelected,
                )
                DockItem.Settings -> SimpleHero(
                    title = stringResource(R.string.settings),
                    subtitle = stringResource(R.string.settings_subtitle),
                    colors = listOf(Color(0xFF26485A), Color(0xFF0F1B24), Color.Black),
                    onOpen = openSelected,
                )
            }
        }

        ClockBadge(Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 44.dp))

        Dock(
            items = items,
            selectedKey = selected.key,
            requester = requester,
            onFocus = { vm.homeSelected = it.key },
            onClick = { item ->
                when (item) {
                    is DockItem.App -> vm.launch(context, item.entry)
                    DockItem.AllApps -> vm.screen = Screen.AllApps
                    DockItem.Settings -> vm.screen = Screen.Settings
                }
            },
            onLongClick = { item -> if (item is DockItem.App) menuFor = item.entry },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        menuFor?.let { app -> PinMenu(app, vm) { menuFor = null } }
    }
}

// ---------------------------------------------------------------- dock

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Dock(
    items: List<DockItem>,
    selectedKey: String,
    requester: (String) -> FocusRequester,
    onFocus: (DockItem) -> Unit,
    onClick: (DockItem) -> Unit,
    onLongClick: (DockItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(DockHeight)
            .focusRestorer { requester(selectedKey) },
        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
        contentPadding = PaddingValues(start = 56.dp, end = 56.dp, top = 24.dp),
    ) {
        items(items, key = { it.key }) { item ->
            DockTile(
                item = item,
                requester = requester(item.key),
                onFocus = { onFocus(item) },
                onClick = { onClick(item) },
                onLongClick = { onLongClick(item) },
            )
        }
    }
}

@Composable
private fun DockTile(
    item: DockItem,
    requester: FocusRequester,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val width = if (item is DockItem.App) 136.dp else 76.dp
    val label = when (item) {
        is DockItem.App -> item.entry.label
        DockItem.AllApps -> stringResource(R.string.all_apps)
        DockItem.Settings -> stringResource(R.string.settings)
    }
    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .size(width, 76.dp)
                .focusRequester(requester)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocus()
                },
            shape = ClickableSurfaceDefaults.shape(TileShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.22f),
            border = tileBorder(),
            colors = tileColors(),
        ) {
            when (item) {
                is DockItem.App -> AppArt(item.entry)
                DockItem.AllApps -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GridGlyph() }
                DockItem.Settings -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, contentDescription = label, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val alpha by animateFloatAsState(if (focused) 1f else 0f, label = "label")
        Text(
            label,
            modifier = Modifier.alpha(alpha).wrapContentWidth(unbounded = true),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun PinMenu(app: AppEntry, vm: LauncherViewModel, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(380.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1F26))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(app.label, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(6.dp))
            MenuRow(stringResource(R.string.move_left), Modifier.focusRequester(first)) {
                vm.movePin(app.packageName, -1); onDismiss()
            }
            MenuRow(stringResource(R.string.move_right)) { vm.movePin(app.packageName, 1); onDismiss() }
            MenuRow(stringResource(R.string.remove_from_home)) { vm.togglePin(app.packageName); onDismiss() }
            MenuRow(stringResource(R.string.cancel)) { onDismiss() }
        }
    }
    LaunchedEffect(Unit) {
        delay(80)
        runCatching { first.requestFocus() }
    }
}

@Composable
fun MenuRow(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.06f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Text(text, Modifier.padding(horizontal = 18.dp, vertical = 14.dp), style = MaterialTheme.typography.titleSmall)
    }
}

// ---------------------------------------------------------------- heroes

@Composable
private fun Scrims() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.85f), 0.6f to Color.Black.copy(alpha = 0.25f), 1f to Color.Transparent)
        )
    )
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.92f))))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.2f to Color.Transparent)))
}

@Composable
private fun HeroColumn(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 56.dp, end = 56.dp, bottom = DockHeight + 4.dp)
        ) { content() }
    }
}

@Composable
private fun OpenButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FeedHero(app: AppEntry, feed: ProviderFeed, onOpen: () -> Unit, onTitle: (Title) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = Tmdb.image(feed.hero.backdrop, "w1280"),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Scrims()
        HeroColumn {
            Text(
                stringResource(R.string.trending_on, feed.provider.name),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(8.dp))
            if (feed.logoPath != null) {
                AsyncImage(
                    model = Tmdb.image(feed.logoPath, "w500"),
                    contentDescription = feed.hero.name,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.height(84.dp).widthIn(max = 400.dp),
                )
            } else {
                Text(
                    feed.hero.name,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                feed.hero.overview,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 560.dp),
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpenButton(stringResource(R.string.open_app, app.label), onOpen)
                val detail = when {
                    feed.episodes != null -> pluralStringResource(R.plurals.episodes, feed.episodes, feed.episodes)
                    feed.runtimeMin != null -> stringResource(R.string.runtime_fmt, feed.runtimeMin / 60, feed.runtimeMin % 60)
                    else -> null
                }
                if (detail != null) {
                    Spacer(Modifier.width(12.dp))
                    InfoChip(detail)
                }
            }
            if (feed.more.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.more_on, feed.provider.name),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(end = 56.dp)) {
                    items(feed.more, key = { it.type + it.id }) { t -> TitleCard(t) { onTitle(t) } }
                }
            }
        }
    }
}

@Composable
private fun TitleCard(t: Title, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(172.dp).aspectRatio(16f / 9f),
        shape = ClickableSurfaceDefaults.shape(TileShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        border = tileBorder(),
        colors = tileColors(),
    ) {
        AsyncImage(
            model = Tmdb.image(t.backdrop, "w300"),
            contentDescription = t.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
        Text(
            t.name,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppHero(app: AppEntry, showTmdbHint: Boolean, onOpen: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(app.color.darken(0.3f), app.color.darken(0.8f), Color.Black)))
    ) {
        val art = app.banner ?: app.icon
        Image(
            art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.65f)
                .alpha(0.35f)
                .blur(28.dp),
        )
        Scrims()
        HeroColumn {
            Box(Modifier.size(224.dp, 126.dp).clip(TileShape)) { AppArt(app) }
            Spacer(Modifier.height(18.dp))
            Text(app.label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
            if (showTmdbHint) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.tmdb_hint, app.label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            OpenButton(stringResource(R.string.open_app, app.label), onOpen)
        }
    }
}

@Composable
private fun SimpleHero(title: String, subtitle: String, colors: List<Color>, onOpen: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors))) {
        Scrims()
        HeroColumn {
            Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.75f))
            Spacer(Modifier.height(16.dp))
            OpenButton(stringResource(R.string.open), onOpen)
        }
    }
}
