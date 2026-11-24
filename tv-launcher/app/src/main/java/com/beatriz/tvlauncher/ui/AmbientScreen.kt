package com.beatriz.tvlauncher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.beatriz.tvlauncher.R
import com.beatriz.tvlauncher.data.DailyPhoto
import com.beatriz.tvlauncher.data.Weather
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Idle screen: today's landscape photo (or a live sky), a big clock, the photo's place and the weather. */
@Composable
fun AmbientScreen(weather: Weather?, photo: DailyPhoto.Photo? = null) {
    val now = rememberNow()
    val minuteOfDay = now.hour * 60 + now.minute
    val sunrise = weather?.sunriseMin ?: (6 * 60)
    val sunset = weather?.sunsetMin ?: (18 * 60)
    val cloudy = (weather?.code ?: 0) >= 3

    val drift by rememberInfiniteTransition(label = "clouds").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(180_000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )
    val skyline = remember { Skyline.generate(Random(7)) }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawSky(minuteOfDay, sunrise, sunset, cloudy, drift, skyline)
        }
        if (photo != null) {
            AsyncImage(
                model = photo.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))
            )
        )
        // Small shift every minute so a static clock never burns into the panel.
        val shift = (now.minute % 6) * 5
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 56.dp, bottom = 40.dp)
                .offset(x = shift.dp, y = (-(now.minute % 4) * 4).dp)
        ) {
            Text(
                timeText(now),
                fontSize = 92.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE3EEF7),
                lineHeight = 96.sp,
                letterSpacing = 1.sp,
            )
            val place = photo?.caption?.takeIf { it.isNotBlank() } ?: weather?.city
            if (place != null) {
                Text(place, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            val line = listOfNotNull(
                weather?.city,
                weather?.let { "${it.tempC}°" },
                weather?.let { stringResource(weatherText(it.code)) },
            ).joinToString(" · ").ifBlank { dateText(now) }
            Text(line, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

fun weatherText(code: Int): Int = when (code) {
    0 -> R.string.wx_clear
    1, 2 -> R.string.wx_partly
    3 -> R.string.wx_cloudy
    45, 48 -> R.string.wx_fog
    in 51..57 -> R.string.wx_drizzle
    in 61..67 -> R.string.wx_rain
    in 71..77, 85, 86 -> R.string.wx_snow
    in 80..82 -> R.string.wx_showers
    in 95..99 -> R.string.wx_storm
    else -> R.string.wx_cloudy
}

// ---------------------------------------------------------------- sky

private data class SkyKey(val minute: Int, val top: Color, val horizon: Color)

private val NIGHT_TOP = Color(0xFF060A18)
private val NIGHT_HOR = Color(0xFF1B2440)

private fun skyColors(m: Int, sunrise: Int, sunset: Int): Pair<Color, Color> {
    val keys = listOf(
        SkyKey(sunrise - 80, NIGHT_TOP, NIGHT_HOR),
        SkyKey(sunrise - 10, Color(0xFF2B3A67), Color(0xFFE58C6A)),
        SkyKey(sunrise + 50, Color(0xFF4F7FBF), Color(0xFFF6C995)),
        SkyKey(sunrise + 140, Color(0xFF2F6FB8), Color(0xFFA9CFEF)),
        SkyKey(sunset - 120, Color(0xFF3368A8), Color(0xFFB9D3EC)),
        SkyKey(sunset - 40, Color(0xFF4B5D8E), Color(0xFFF7C27A)),
        SkyKey(sunset + 5, Color(0xFF3A2D5C), Color(0xFFF08A5D)),
        SkyKey(sunset + 70, NIGHT_TOP, NIGHT_HOR),
    )
    if (m <= keys.first().minute || m >= keys.last().minute) return NIGHT_TOP to NIGHT_HOR
    val i = keys.indexOfLast { it.minute <= m }
    val a = keys[i]
    val b = keys[i + 1]
    val t = (m - a.minute).toFloat() / (b.minute - a.minute).coerceAtLeast(1)
    return lerp(a.top, b.top, t) to lerp(a.horizon, b.horizon, t)
}

private class Skyline(
    val front: List<FloatArray>,
    val back: List<FloatArray>,
    val windows: List<Offset>,
    val stars: List<Offset>,
) {
    companion object {
        /** Building = [x, width, height] as fractions of the screen. */
        private fun layer(r: Random, minH: Float, maxH: Float, tallChance: Float, tallMax: Float): List<FloatArray> {
            val b = ArrayList<FloatArray>()
            var x = -0.01f
            while (x < 1.02f) {
                val w = 0.018f + r.nextFloat() * 0.045f
                val h = if (r.nextFloat() < tallChance) maxH + r.nextFloat() * (tallMax - maxH) else minH + r.nextFloat() * (maxH - minH)
                b += floatArrayOf(x, w + 0.002f, h) // slight overlap: no hairline gaps
                x += w
            }
            return b
        }

        fun generate(r: Random): Skyline = Skyline(
            front = layer(r, 0.035f, 0.10f, 0.10f, 0.19f),
            back = layer(r, 0.07f, 0.15f, 0.12f, 0.24f),
            windows = List(160) { Offset(r.nextFloat(), r.nextFloat()) },
            stars = List(90) { Offset(r.nextFloat(), r.nextFloat() * 0.6f) },
        )
    }
}

private fun DrawScope.drawSky(m: Int, sunrise: Int, sunset: Int, cloudy: Boolean, drift: Float, sky: Skyline) {
    var (top, horizon) = skyColors(m, sunrise, sunset)
    if (cloudy) {
        top = lerp(top, Color(0xFF59606B), 0.35f)
        horizon = lerp(horizon, Color(0xFF8A9099), 0.35f)
    }
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(0f to top, 0.75f to horizon, 1f to horizon))

    val isDay = m in sunrise..sunset
    val nightness = when {
        m < sunrise - 60 || m > sunset + 60 -> 1f
        m < sunrise -> (sunrise - m) / 60f
        m > sunset -> (m - sunset) / 60f
        else -> 0f
    }

    // Stars
    if (nightness > 0f && !cloudy) {
        sky.stars.forEachIndexed { i, s ->
            drawCircle(Color.White.copy(alpha = nightness * (0.35f + (i % 5) * 0.12f)), radius = 1.2f + (i % 3), center = Offset(s.x * w, s.y * h))
        }
    }

    // Sun (day) or moon (night) along an arc
    val t: Float
    val bodyColor: Color
    if (isDay) {
        t = (m - sunrise).toFloat() / (sunset - sunrise).coerceAtLeast(1)
        bodyColor = Color(0xFFFFE2A8)
    } else {
        val nightLen = (24 * 60 - sunset + sunrise).coerceAtLeast(1)
        val since = if (m > sunset) m - sunset else m + 24 * 60 - sunset
        t = since.toFloat() / nightLen
        bodyColor = Color(0xFFE8ECF5)
    }
    val center = Offset(w * (0.34f + 0.6f * t), h * (0.78f - 0.58f * sin(PI * t).toFloat()))
    val glow = if (isDay) h * 0.45f else h * 0.18f
    drawCircle(
        Brush.radialGradient(
            listOf(bodyColor.copy(alpha = if (cloudy) 0.25f else 0.55f), Color.Transparent),
            center = center, radius = glow,
        ),
        radius = glow, center = center,
    )
    if (!cloudy) drawCircle(bodyColor, radius = if (isDay) h * 0.035f else h * 0.025f, center = center)

    // Soft drifting clouds
    val cloudCount = if (cloudy) 9 else 4
    for (i in 0 until cloudCount) {
        val speed = 0.6f + (i % 3) * 0.25f
        val cx = (((i * 0.37f) + drift * speed) % 1.3f - 0.15f) * w
        val cy = h * (0.12f + (i * 0.083f) % 0.45f)
        val rw = w * (0.18f + (i % 4) * 0.05f)
        val rh = rw * 0.28f
        val alpha = if (cloudy) 0.22f else 0.12f
        drawOval(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = alpha * (1f - nightness * 0.85f)), Color.Transparent),
                center = Offset(cx, cy), radius = rw / 2,
            ),
            topLeft = Offset(cx - rw / 2, cy - rh / 2),
            size = Size(rw, rh),
        )
    }

    // City silhouette: a hazy far layer and a darker near layer
    val nearColor = lerp(lerp(horizon, Color.Black, 0.75f), Color(0xFF05070D), nightness * 0.6f)
    val farColor = lerp(horizon, nearColor, 0.45f)
    sky.back.forEach { (bx, bw, bh) ->
        drawRect(farColor, topLeft = Offset(bx * w, h * (1f - bh)), size = Size(bw * w, h * bh))
    }
    sky.front.forEach { (bx, bw, bh) ->
        drawRect(nearColor, topLeft = Offset(bx * w, h * (1f - bh)), size = Size(bw * w, h * bh))
    }
    // Lit windows after dusk
    if (nightness > 0.05f) {
        sky.windows.forEachIndexed { i, p ->
            val layer = if (i % 2 == 0) sky.front else sky.back
            val b = layer[i % layer.size]
            val x = (b[0] + 0.15f * b[1] + p.x * 0.7f * b[1]) * w
            val y = h * (1f - b[2] * (0.1f + p.y * 0.8f))
            val a = if (layer === sky.front) 0.6f else 0.3f
            drawRect(Color(0xFFFFD27A).copy(alpha = a * nightness), topLeft = Offset(x, y), size = Size(3f, 4f))
        }
    }
    // Bottom fade so the text stays readable
    drawRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f)))
}
