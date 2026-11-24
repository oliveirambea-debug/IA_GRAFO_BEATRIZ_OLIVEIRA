package com.beatriz.tvlauncher.data

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette

data class AppEntry(
    val packageName: String,
    val label: String,
    val banner: ImageBitmap?,
    val icon: ImageBitmap,
    val color: Color,
    val intent: Intent,
)

object AppRepository {

    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val result = LinkedHashMap<String, AppEntry>()
        // TV apps first (they have proper banners), then phone-style apps.
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo
                val pkg = ai.packageName
                if (pkg == context.packageName || result.containsKey(pkg)) continue
                runCatching {
                    val launch = Intent(Intent.ACTION_MAIN)
                        .addCategory(category)
                        .setClassName(pkg, ai.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    val banner = ai.loadBanner(pm)?.toBitmap(320, 180)
                    val icon = ri.loadIcon(pm).toBitmap(160, 160)
                    val palette = Palette.from(banner ?: icon).generate()
                    val color = palette.getVibrantColor(palette.getDominantColor(0xFF33415C.toInt()))
                    result[pkg] = AppEntry(
                        packageName = pkg,
                        label = ri.loadLabel(pm).toString(),
                        banner = banner?.asImageBitmap(),
                        icon = icon.asImageBitmap(),
                        color = Color(color),
                        intent = launch,
                    )
                }
            }
        }
        return result.values.sortedBy { it.label.lowercase() }
    }

    /** First-run home row: known streaming apps first, then fill up with whatever is installed. */
    fun defaultPins(apps: List<AppEntry>): List<String> {
        val preferred = listOf(
            "com.google.android.youtube.tv", "com.spotify.tv.android",
        )
        val streaming = Providers.ALL.flatMap { p -> apps.filter { Providers.match(it) == p } }
        val pins = LinkedHashSet<String>()
        streaming.forEach { pins += it.packageName }
        preferred.filter { pkg -> apps.any { it.packageName == pkg } }.forEach { pins += it }
        val skip = listOf("settings", "store", "play", "update", "setup")
        apps.filter { a -> skip.none { a.label.lowercase().contains(it) || a.packageName.contains(it) } }
            .forEach { if (pins.size < 6) pins += it.packageName }
        return pins.toList()
    }
}
