package com.beatriz.tvlauncher

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import com.beatriz.tvlauncher.data.AppEntry
import com.beatriz.tvlauncher.data.ProviderFeed
import com.beatriz.tvlauncher.data.Providers
import com.beatriz.tvlauncher.data.Title
import com.beatriz.tvlauncher.data.Weather
import com.beatriz.tvlauncher.ui.LauncherApp
import com.beatriz.tvlauncher.ui.LauncherTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.time.LocalTime

/**
 * Renders each screen to build/screenshots/ with fake apps and placeholder art.
 * Run: ./gradlew testDebugUnitTest
 */
object DockItemKeys { const val ALL_APPS = "__all_apps" }

@OptIn(ExperimentalCoilApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun fakeImages() {
        val engine = FakeImageLoaderEngine.Builder()
            .intercept({ it is String && it.contains("bing.com") }, BitmapDrawable(app.resources, art(1920, 1080, 0xFF6B7FA8.toInt(), 0xFFE8A86B.toInt(), "")))
            .intercept({ it is String && it.contains("/w1280") }, BitmapDrawable(app.resources, art(1280, 720, 0xFF2E4A62.toInt(), 0xFFD9A066.toInt(), "")))
            .intercept({ it is String && it.contains("/w500") }, BitmapDrawable(app.resources, logo("NORTHWIND")))
            .default(BitmapDrawable(app.resources, art(300, 169, 0xFF5B3A7A.toInt(), 0xFF1D8A99.toInt(), "")))
            .build()
        Coil.setImageLoader(ImageLoader.Builder(app).components { add(engine) }.build())
    }

    private fun art(w: Int, h: Int, c1: Int, c2: Int, text: String): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), c1, c2, Shader.TileMode.CLAMP) })
        if (text.isNotEmpty()) {
            c.drawText(text, w / 2f, h / 2f + h * 0.1f, Paint().apply {
                color = android.graphics.Color.WHITE; textSize = h * 0.3f; textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
            })
        }
        return b
    }

    private fun logo(text: String): Bitmap {
        val b = Bitmap.createBitmap(820, 120, Bitmap.Config.ARGB_8888)
        Canvas(b).drawText(text, 0f, 95f, Paint().apply {
            color = 0xFFF5C542.toInt(); textSize = 96f; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); isAntiAlias = true
        })
        return b
    }

    private fun fakeApp(pkg: String, label: String, c1: Int, c2: Int, hasBanner: Boolean = true) = AppEntry(
        packageName = pkg,
        label = label,
        banner = if (hasBanner) art(320, 180, c1, c2, label).asImageBitmap() else null,
        icon = art(160, 160, c1, c2, label.take(1)).asImageBitmap(),
        color = Color(c1),
        intent = Intent(),
    )

    private fun viewModel(selected: String?): LauncherViewModel {
        val vm = LauncherViewModel(app, autoStart = false)
        val apps = listOf(
            fakeApp("com.netflix.ninja", "Netflix", 0xFF141414.toInt(), 0xFFB20710.toInt()),
            fakeApp("com.amazon.amazonvideo.livingroom", "Prime Video", 0xFF00A8E1.toInt(), 0xFF146EB4.toInt()),
            fakeApp("com.disney.disneyplus", "Disney+", 0xFF0B1F59.toInt(), 0xFF1BA5C4.toInt()),
            fakeApp("com.globo.globotv", "Globoplay", 0xFFFF4A3D.toInt(), 0xFFC81E5B.toInt()),
            fakeApp("com.google.android.youtube.tv", "YouTube", 0xFFFFFFFF.toInt(), 0xFFFF0000.toInt()),
            fakeApp("com.spotify.tv.android", "Spotify", 0xFF191414.toInt(), 0xFF1DB954.toInt(), hasBanner = false),
            fakeApp("com.example.photos", "Photos", 0xFF3367D6.toInt(), 0xFF34A853.toInt()),
            fakeApp("com.example.weather", "Weather", 0xFF4A90E2.toInt(), 0xFF50E3C2.toInt(), hasBanner = false),
        )
        vm.apps = apps.sortedBy { it.label }
        vm.pinned = apps.take(6).map { it.packageName }
        vm.homeSelected = selected
        val netflix = Providers.ALL.first { it.name == "Netflix" }
        vm.feeds[netflix.name] = ProviderFeed(
            provider = netflix,
            hero = Title(1, "tv", "Northwind", "A fishing town on the edge of the North Atlantic hides a secret that the sea keeps bringing back, one tide at a time.", "/hero.jpg", 100.0),
            logoPath = "/logo.png",
            seasons = 1,
            episodes = 6,
            runtimeMin = null,
            more = (2..9).map { Title(it, "tv", "Title $it", "", "/b$it.jpg", 50.0) },
        )
        return vm
    }

    private fun capture(name: String, vm: LauncherViewModel, autoAdvance: Boolean = true) {
        compose.mainClock.autoAdvance = autoAdvance
        compose.setContent { LauncherTheme { LauncherApp(vm) } }
        repeat(5) {
            compose.mainClock.advanceTimeBy(500)
            ShadowLooper.idleMainLooper()
        }
        if (autoAdvance) compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test fun home_streaming() = capture("1_home_netflix", viewModel("com.netflix.ninja"))

    @Test fun home_app() = capture("2_home_youtube", viewModel("com.google.android.youtube.tv"))

    @Test fun home_all_apps_card() = capture("2b_home_all_apps_card", viewModel(DockItemKeys.ALL_APPS))

    @Test fun all_apps() = capture("3_all_apps", viewModel(null).also { it.screen = Screen.AllApps })

    @Test fun settings() = capture("4_settings", viewModel(null).also { it.screen = Screen.Settings })

    private fun ambient(name: String, sunriseOffset: Int, sunsetOffset: Int, code: Int) {
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        val vm = viewModel(null)
        vm.weather = Weather("Lisboa", 18, code, now + sunriseOffset, now + sunsetOffset)
        vm.ambient = true
        capture(name, vm, autoAdvance = false)
    }

    @Test fun ambient_sunset() = ambient("5_ambient_sunset", -600, 15, 0)

    @Test fun ambient_day() = ambient("6_ambient_day", -300, 300, 2)

    @Test fun ambient_photo() {
        val vm = viewModel(null)
        vm.weather = Weather("Lisboa", 18, 0, 6 * 60, 18 * 60)
        vm.photoUrl = "https://www.bing.com/th?id=photo_1920x1080.jpg"
        vm.ambient = true
        capture("8_ambient_photo", vm, autoAdvance = false)
    }

    @Test fun ambient_night() = ambient("7_ambient_night", 400, -200, 0)
}
