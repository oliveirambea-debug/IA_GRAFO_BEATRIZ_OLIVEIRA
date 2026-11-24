package com.beatriz.tvlauncher.data

/** Streaming services whose catalogue we can show via TMDB "watch providers". */
data class Provider(
    val name: String,
    /** TMDB watch-provider ids, joined with '|' (OR) in queries. */
    val tmdbIds: String,
    val packages: List<String>,
    val keywords: List<String>,
)

object Providers {
    val ALL = listOf(
        Provider("Netflix", "8", listOf("com.netflix.ninja", "com.netflix.mediaclient"), listOf("netflix")),
        Provider("Prime Video", "119|9", listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"), listOf("prime video")),
        Provider("Disney+", "337", listOf("com.disney.disneyplus"), listOf("disney")),
        Provider("Max", "1899|384", listOf("com.wbd.stream", "com.hbo.hbonow", "com.hbo.max.android.tv"), listOf("hbo")),
        Provider("Globoplay", "307", listOf("com.globo.globotv"), listOf("globoplay")),
        Provider("Apple TV", "350", listOf("com.apple.atve.androidtv.appletv"), listOf("apple tv")),
        Provider("Paramount+", "531", listOf("com.cbs.ott", "com.cbs.app"), listOf("paramount")),
    )

    fun match(app: AppEntry): Provider? {
        val label = app.label.lowercase()
        return ALL.firstOrNull { app.packageName in it.packages }
            ?: ALL.firstOrNull { p -> p.keywords.any { label.contains(it) } }
            ?: ALL.firstOrNull { it.name == "Max" && label == "max" }
    }
}
