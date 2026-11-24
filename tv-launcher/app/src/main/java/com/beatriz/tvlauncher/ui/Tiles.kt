package com.beatriz.tvlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import com.beatriz.tvlauncher.data.AppEntry

val TileShape = RoundedCornerShape(14.dp)

@Composable
fun tileBorder() = ClickableSurfaceDefaults.border(
    focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = TileShape),
)

@Composable
fun tileColors() = ClickableSurfaceDefaults.colors(
    containerColor = Color.White.copy(alpha = 0.12f),
    focusedContainerColor = Color.White.copy(alpha = 0.22f),
    pressedContainerColor = Color.White.copy(alpha = 0.3f),
)

/** The app's TV banner, or its icon on a tinted card when it has no banner. */
@Composable
fun AppArt(app: AppEntry, modifier: Modifier = Modifier) {
    if (app.banner != null) {
        Image(app.banner, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = modifier.fillMaxSize())
    } else {
        Box(
            modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(app.color.copy(alpha = 0.9f), app.color.darken(0.55f)))),
            contentAlignment = Alignment.Center,
        ) {
            Image(app.icon, contentDescription = app.label, modifier = Modifier.size(48.dp))
        }
    }
}

/** 3x3 dots: the "all apps" symbol. */
@Composable
fun GridGlyph(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier.size(26.dp)) {
        val r = size.minDimension / 10f
        for (i in 0..2) for (j in 0..2) {
            drawCircle(color, r, Offset(size.width * (0.17f + i * 0.33f), size.height * (0.17f + j * 0.33f)))
        }
    }
}

fun Color.darken(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
