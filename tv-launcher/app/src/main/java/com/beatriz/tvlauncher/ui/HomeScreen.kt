package com.beatriz.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.beatriz.tvlauncher.LauncherViewModel
import com.beatriz.tvlauncher.R
import com.beatriz.tvlauncher.Screen
import com.beatriz.tvlauncher.data.AppEntry
import com.beatriz.tvlauncher.data.CardItem
import com.beatriz.tvlauncher.data.CardTile
import com.beatriz.tvlauncher.data.ProviderFeed
import com.beatriz.tvlauncher.data.Providers
import com.beatriz.tvlauncher.data.Title
import com.beatriz.tvlauncher.data.Tmdb
import com.beatriz.tvlauncher.data.Weather
import com.beatriz.tvlauncher.data.WidgetCard
import kotlinx.coroutines.delay
import java.util.Locale

// Layout measured from the reference video (TV = 960 x 540 dp).
private val DockHeight: Dp = 150.dp
private val DockTileSize: Dp = 58.dp
private val DockGap: Dp = 30.dp
private val Cream = Color(0xFFEDEAE3)
private val CardBg = Color(0xFF1C1E24)

sealed class DockItem(val key: String) {
    data object AllApps : DockItem("__all_apps")
    data object Settings : DockItem("__settings")
    data class App(val entry: AppEntry) : DockItem(entry.packageName)
}

private data class HeroState(val item: DockItem, val feed: ProviderFeed?, val card: WidgetCard?)

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val dockApps = vm.dockApps
    val items = remember(dockApps) {
        listOf<DockItem>(DockItem.AllApps) + dockApps.map { DockItem.App(it) } + DockItem.Settings
    }
    val selectedIndex = items.indexOfFirst { it.key == vm.homeSelected }.takeIf { it >= 0 } ?: if (items.size > 2) 1 else 0
    val selected = items[selectedIndex]
    val requesters = remember { HashMap<String, FocusRequester>() }
    val requester = { key: String -> requesters.getOrPut(key) { FocusRequester() } }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }

    BackHandler(enabled = true) { /* this is home: nothing to go back to */ }

    LaunchedEffect(selected.key) {
        delay(250)
        (selected as? DockItem.App)?.let {
            vm.requestFeed(it.entry)
            vm.requestCard(it.entry)
        }
    }
    LaunchedEffect(items.size) {
        delay(60)
        runCatching { requester(selected.key).requestFocus() }
    }

    val open: (DockItem) -> Unit = { item ->
        when (item) {
            is DockItem.App -> vm.launch(context, item.entry)
            DockItem.AllApps -> vm.screen = Screen.AllApps
            DockItem.Settings -> vm.screen = Screen.Settings
        }
    }

    val app = (selected as? DockItem.App)?.entry
    val state = HeroState(selected, app?.let { vm.feedFor(it) }, app?.let { vm.cardFor(it) })

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(400)) },
            contentKey = { it.item.key + ":" + (it.feed?.hero?.id ?: 0) + ":" + (it.card != null) },
            label = "hero",
        ) { s ->
            if (s.feed != null && s.item is DockItem.App) {
                FeedHero(s.item.entry, s.feed) { open(s.item) }
            } else {
                CardScene(s, vm)
            }
        }

        ClockBadge(Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 34.dp))

        Dock(
            items = items,
            selectedIndex = selectedIndex,
            requester = requester,
            onFocus = { vm.homeSelected = it.key },
            onClick = open,
            onLongClick = { item -> if (item is DockItem.App) menuFor = item.entry },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        menuFor?.let { a -> PinMenu(a, vm) { menuFor = null } }
    }
}

// ---------------------------------------------------------------- dock (carousel: the focused app stays centred)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Dock(
    items: List<DockItem>,
    selectedIndex: Int,
    requester: (String) -> FocusRequester,
    onFocus: (DockItem) -> Unit,
    onClick: (DockItem) -> Unit,
    onLongClick: (DockItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(DockHeight)) {
        val target = maxWidth / 2 - DockTileSize / 2 - (DockTileSize + DockGap) * selectedIndex
        val x by animateDpAsState(target, tween(380, easing = FastOutSlowInEasing), label = "dock")
        Row(
            Modifier
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .offset(x = x)
                .padding(top = 14.dp)
                .focusRestorer { requester(items[selectedIndex].key) },
            horizontalArrangement = Arrangement.spacedBy(DockGap),
        ) {
            items.forEach { item ->
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
    val label = when (item) {
        is DockItem.App -> item.entry.label
        DockItem.AllApps -> stringResource(R.string.all_apps)
        DockItem.Settings -> stringResource(R.string.settings)
    }
    val shape = RoundedCornerShape(15.dp)
    Column(Modifier.width(DockTileSize), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .size(DockTileSize)
                .focusRequester(requester)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocus()
                },
            shape = ClickableSurfaceDefaults.shape(shape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.34f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.95f)), shape = shape),
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF3A3D47),
                focusedContainerColor = Color(0xFF3A3D47),
                pressedContainerColor = Color(0xFF4A4D57),
            ),
        ) {
            Box(Modifier.fillMaxSize().alpha(if (focused) 1f else 0.88f), contentAlignment = Alignment.Center) {
                when (item) {
                    is DockItem.App -> Image(item.entry.icon, label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    DockItem.AllApps -> GridGlyph()
                    DockItem.Settings -> Icon(Icons.Filled.Settings, label, tint = Color(0xFFEDE6D6), modifier = Modifier.size(26.dp))
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        val alpha by animateFloatAsState(if (focused) 1f else 0f, label = "label")
        Text(
            label,
            modifier = Modifier.alpha(alpha).wrapContentWidth(unbounded = true),
            fontSize = 12.sp,
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

// ---------------------------------------------------------------- shared bits

@Composable
private fun NetImage(url: String?, fallback: String?, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.06f))) },
        error = {
            if (fallback != null) {
                AsyncImage(fallback, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.06f)))
            }
        },
    )
}

/** Cream pill with ▶ (primary action) – the look of the reference's "Ouvrir …" buttons. */
@Composable
private fun PrimaryPill(text: String, small: Boolean = false) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Cream)
            .padding(horizontal = if (small) 12.dp else 16.dp, vertical = if (small) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF26262A), modifier = Modifier.size(if (small) 11.dp else 13.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = if (small) 10.sp else 12.sp, color = Color(0xFF26262A), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SecondaryLabel(text: String, icon: ImageVector = Icons.AutoMirrored.Filled.List) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
    }
}

// ---------------------------------------------------------------- card scene (YouTube, music, all apps, other apps)

@Composable
private fun CardScene(s: HeroState, vm: LauncherViewModel) {
    val item = s.item
    val app = (item as? DockItem.App)?.entry
    val tint = app?.color ?: when (item) {
        DockItem.Settings -> Color(0xFF2E5A6E)
        else -> Color(0xFF26272C)
    }
    Box(Modifier.fillMaxSize()) {
        BlurredBackdrop(s.card?.tiles?.firstOrNull()?.image, tint)
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val shape = RoundedCornerShape(28.dp)
            Box(
                Modifier
                    .width(590.dp)
                    .height(282.dp)
                    .clip(shape)
                    .background(CardBg.copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
                    .padding(12.dp)
            ) {
                when {
                    s.card != null -> TilesCard(s.card)
                    item is DockItem.AllApps -> BannerCard(
                        stringResource(R.string.all_your_apps),
                        stringResource(R.string.all_apps_card, vm.apps.size),
                        vm.apps.take(5),
                    )
                    item is DockItem.Settings -> SettingsCard(vm.weather)
                    app != null -> BannerCard(
                        app.label,
                        if (Providers.match(app) != null && vm.prefs.tmdbKey.isBlank()) stringResource(R.string.tmdb_hint, app.label)
                        else stringResource(R.string.press_ok),
                        listOf(app),
                        big = true,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(42.dp, 3.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.5f)))
            Spacer(Modifier.height(10.dp))
            val primary = when (item) {
                is DockItem.App -> stringResource(R.string.open_app, item.entry.label)
                DockItem.AllApps -> stringResource(R.string.all_apps)
                DockItem.Settings -> stringResource(R.string.settings)
            }
            val secondary = s.card?.pillSecondary ?: when (item) {
                DockItem.AllApps -> stringResource(R.string.apps_count, vm.apps.size)
                DockItem.Settings -> vm.weather?.let { "${it.city} · ${it.tempC}°" }
                else -> null
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A2C31).copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryPill(primary, small = true)
                if (secondary != null) {
                    Box(Modifier.padding(horizontal = 12.dp)) { SecondaryLabel(secondary) }
                }
            }
        }
    }
}

/** The background is the card's own artwork, heavily blurred (decoded tiny and scaled up), with a vignette. */
@Composable
private fun BlurredBackdrop(image: String?, tint: Color) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color(0xFF07080A))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(tint.copy(alpha = 0.75f), tint.darken(0.6f).copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.75f),
                    radius = size.maxDimension * 0.7f,
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(tint.darken(0.2f).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.25f),
                    radius = size.maxDimension * 0.5f,
                )
            )
        }
        if (image != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(image).size(24, 14).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize().alpha(0.8f),
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    center = Offset(size.width * 0.55f, size.height * 0.45f),
                    radius = size.maxDimension * 0.75f,
                )
            )
        }
    }
}

@Composable
private fun TilesCard(card: WidgetCard) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val weights = listOf(1.5f, 1f, 0.95f)
            card.tiles.take(3).forEachIndexed { i, t ->
                ContentTile(t, Modifier.weight(weights[i]).fillMaxHeight())
            }
        }
        if (card.row.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            if (card.rowLabel != null) {
                Text(card.rowLabel, Modifier.padding(start = 6.dp), fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(6.dp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                card.row.take(4).forEach { RowItem(it, Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ContentTile(t: CardTile, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF2A2C33))) {
        NetImage(t.image, t.fallbackImage, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f))
            )
        )
        if (t.tag != null) {
            Text(
                t.tag.uppercase(Locale.getDefault()),
                Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = Color.White,
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 10.dp, bottom = 11.dp)) {
            Text(t.kicker, fontSize = 9.5.sp, lineHeight = 12.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(t.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.meta, fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RowItem(it: CardItem, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val thumb = if (it.square) Modifier.size(38.dp) else Modifier.size(58.dp, 33.dp)
        NetImage(it.image, null, thumb.clip(RoundedCornerShape(6.dp)))
        Spacer(Modifier.width(7.dp))
        Column {
            Text(it.title, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(it.subtitle, fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "Toutes tes applis"-style card: big title, subtitle, a row of app banners. */
@Composable
private fun BannerCard(title: String, subtitle: String, apps: List<AppEntry>, big: Boolean = false) {
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp), verticalArrangement = Arrangement.Center) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f), maxLines = 2)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            apps.forEach { a ->
                val size = if (big) Modifier.size(200.dp, 112.dp) else Modifier.size(92.dp, 52.dp)
                Box(size.clip(RoundedCornerShape(8.dp))) { AppArt(a) }
            }
        }
    }
}

@Composable
private fun SettingsCard(weather: Weather?) {
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.settings), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.settings_subtitle), fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
        Spacer(Modifier.height(16.dp))
        val place = weather?.let { "${it.city} · ${it.tempC}° · ${stringResource(weatherText(it.code))}" }
        Text(place ?: stringResource(R.string.setting_city_unset), fontSize = 14.sp, color = Color.White)
    }
}

// ---------------------------------------------------------------- streaming hero (Netflix, Prime Video, Disney+…)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FeedHero(app: AppEntry, feed: ProviderFeed, onOpen: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = Tmdb.image(feed.hero.backdrop, "w1280"),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.8f), 0.5f to Color.Black.copy(alpha = 0.25f), 0.8f to Color.Transparent)
            )
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.9f))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 0.15f to Color.Transparent)))

        Column(Modifier.align(Alignment.TopStart).padding(start = 60.dp, top = 108.dp, end = 60.dp)) {
            val country = Locale("", feed.region).getDisplayCountry(Locale.getDefault()).ifBlank { feed.region }
            Text(
                stringResource(R.string.trending_in, feed.provider.name, country),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            if (feed.logoPath != null) {
                AsyncImage(
                    model = Tmdb.image(feed.logoPath, "w500"),
                    contentDescription = feed.hero.name,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.height(62.dp).widthIn(max = 330.dp),
                )
            } else {
                Text(feed.hero.name, fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                feed.hero.overview,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 460.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onOpen,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = RoundedCornerShape(50)),
                    ),
                ) { PrimaryPill(stringResource(R.string.open_app, app.label)) }
                val detail = when {
                    feed.episodes != null -> pluralStringResource(R.plurals.episodes, feed.episodes, feed.episodes)
                    feed.runtimeMin != null -> stringResource(R.string.runtime_fmt, feed.runtimeMin / 60, feed.runtimeMin % 60)
                    else -> null
                }
                if (detail != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) { SecondaryLabel(detail) }
                }
            }
            if (feed.more.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.more_on, feed.provider.name), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 60.dp)) {
                    items(feed.more, key = { it.type + it.id }) { t -> TitleCard(t, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun TitleCard(t: Title, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(136.dp, 76.dp),
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.5.dp, Color.White), shape = shape)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF22242A), focusedContainerColor = Color(0xFF22242A)),
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
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 7.dp, vertical = 5.dp),
            fontSize = 9.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
