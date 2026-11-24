package com.beatriz.tvlauncher.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime

/** Big tile in a home-screen card (image, small tag, kicker line, bold title, meta line). */
data class CardTile(
    val tag: String?,
    val kicker: String,
    val title: String,
    val meta: String,
    val image: String,
    val fallbackImage: String? = null,
)

/** Small item in the card's bottom row. */
data class CardItem(val title: String, val subtitle: String, val image: String, val square: Boolean)

data class WidgetCard(
    val tiles: List<CardTile>,
    val rowLabel: String?,
    val row: List<CardItem>,
    val pillSecondary: String,
)

enum class CardKind { YouTube, Music }

object CardKinds {
    fun of(app: AppEntry): CardKind? {
        val label = app.label.lowercase()
        val pkg = app.packageName
        return when {
            pkg.startsWith("com.google.android.youtube.tvmusic") || label.contains("youtube music") -> CardKind.Music
            pkg.startsWith("com.google.android.youtube") || label == "youtube" -> CardKind.YouTube
            listOf("spotify", "deezer", "tidal", "music", "música", "musica").any { label.contains(it) || pkg.contains(it) } -> CardKind.Music
            else -> null
        }
    }
}

/** YouTube channel feeds (public RSS, no key). */
object YouTubeFeed {
    data class Video(val id: String, val title: String, val channel: String, val published: OffsetDateTime)

    private val ID = Regex("UC[A-Za-z0-9_-]{22}")

    /** Accepts a channel id (UC…), a @handle, or a channel URL. */
    fun resolveChannelId(input: String): String? {
        val s = input.trim()
        ID.find(s)?.let { return it.value }
        val handle = s.substringAfterLast("/").removePrefix("@").ifBlank { return null }
        val html = fetch("https://www.youtube.com/@" + Http.enc(handle))
        return Regex("channel_id=(UC[A-Za-z0-9_-]{22})").find(html)?.groupValues?.get(1)
            ?: Regex("\"externalId\":\"(UC[A-Za-z0-9_-]{22})\"").find(html)?.groupValues?.get(1)
    }

    fun latest(channelId: String): List<Video> =
        parse(fetch("https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"))

    /** Parses the Atom feed, skipping Shorts. */
    fun parse(xml: String): List<Video> {
        val p = Xml.newPullParser()
        p.setInput(StringReader(xml))
        val out = ArrayList<Video>()
        var inEntry = false
        var inAuthor = false
        var id = ""; var title = ""; var channel = ""; var published = ""; var isShort = false
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (p.name.substringAfter(':')) {
                    "entry" -> { inEntry = true; id = ""; title = ""; channel = ""; published = ""; isShort = false }
                    "author" -> inAuthor = true
                    "videoId" -> if (inEntry) id = p.nextText()
                    "title" -> if (inEntry && title.isEmpty()) title = p.nextText()
                    "name" -> if (inEntry && inAuthor) channel = p.nextText()
                    "published" -> if (inEntry) published = p.nextText()
                    "link" -> if (inEntry && p.getAttributeValue(null, "href")?.contains("/shorts/") == true) isShort = true
                }
                XmlPullParser.END_TAG -> when (p.name.substringAfter(':')) {
                    "author" -> inAuthor = false
                    "entry" -> {
                        inEntry = false
                        val date = runCatching { OffsetDateTime.parse(published) }.getOrNull()
                        if (id.isNotEmpty() && !isShort && date != null) out += Video(id, title, channel, date)
                    }
                }
            }
            event = p.next()
        }
        return out
    }

    fun thumb(id: String) = "https://i.ytimg.com/vi/$id/hq720.jpg"
    fun smallThumb(id: String) = "https://i.ytimg.com/vi/$id/mqdefault.jpg"

    private fun fetch(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000
        c.readTimeout = 12_000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12; TV) AppleWebKit/537.36 Chrome/120 Safari/537.36")
        c.setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
        try {
            if (c.responseCode !in 200..299) throw java.io.IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}

/** Apple's public music charts (no key): most played songs/albums per country. */
object MusicCharts {
    data class Entry(val artist: String, val name: String, val art100: String) {
        fun art(size: Int) = art100.replace("100x100bb", "${size}x${size}bb")
    }

    fun top(region: String, kind: String): List<Entry> =
        parse(Http.getJson("https://rss.marketingtools.apple.com/api/v2/${region.lowercase()}/music/most-played/10/$kind.json").toString())

    fun parse(json: String): List<Entry> {
        val results = org.json.JSONObject(json).getJSONObject("feed").getJSONArray("results")
        return (0 until results.length()).map {
            val o = results.getJSONObject(it)
            Entry(o.optString("artistName"), o.optString("name"), o.optString("artworkUrl100"))
        }
    }
}
