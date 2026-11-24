package com.beatriz.tvlauncher

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beatriz.tvlauncher.data.MusicCharts
import com.beatriz.tvlauncher.data.YouTubeFeed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Parses real responses saved from YouTube's channel feed and Apple's music charts. */
@RunWith(AndroidJUnit4::class)
class ParserTest {
    private fun res(name: String) = javaClass.classLoader!!.getResource(name).readText()

    @Test fun youtubeFeed() {
        val videos = YouTubeFeed.parse(res("yt_sample.xml"))
        assertTrue("expected videos", videos.isNotEmpty())
        val v = videos.first()
        assertTrue(v.id.length == 11)
        assertTrue(v.title.isNotBlank())
        assertTrue(v.channel.startsWith("Kurzgesagt"))
        // The sample's newest entry is a Short; Shorts must be skipped.
        assertFalse(videos.any { it.id == "39Hc0MZtE4w" })
    }

    @Test fun musicCharts() {
        val songs = MusicCharts.parse(res("songs_sample.json"))
        assertTrue(songs.size >= 3)
        assertTrue(songs[0].art(600).contains("600x600bb"))
        assertTrue(songs[0].artist.isNotBlank() && songs[0].name.isNotBlank())
    }
}
