package com.beatriz.tvlauncher

import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatriz.tvlauncher.data.AppEntry
import com.beatriz.tvlauncher.data.AppRepository
import com.beatriz.tvlauncher.data.DailyPhoto
import com.beatriz.tvlauncher.data.Prefs
import com.beatriz.tvlauncher.data.ProviderFeed
import com.beatriz.tvlauncher.data.Providers
import com.beatriz.tvlauncher.data.Tmdb
import com.beatriz.tvlauncher.data.Weather
import com.beatriz.tvlauncher.data.WeatherApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class Screen { Home, AllApps, Settings }

class LauncherViewModel @JvmOverloads constructor(
    app: Application,
    /** False only in screenshot tests, which inject their own data. */
    autoStart: Boolean = true,
) : AndroidViewModel(app) {
    val prefs = Prefs(app)

    var apps by mutableStateOf<List<AppEntry>>(emptyList()); internal set
    var pinned by mutableStateOf<List<String>>(emptyList()); internal set
    var weather by mutableStateOf<Weather?>(null); internal set
    var photoUrl by mutableStateOf<String?>(null); internal set
    var screen by mutableStateOf(Screen.Home)
    var ambient by mutableStateOf(false)

    /** Dock item the home screen last had focused (kept while visiting other screens). */
    var homeSelected by mutableStateOf<String?>(null)

    /** Bumped when settings change so the settings screen re-reads prefs. */
    var settingsVersion by mutableStateOf(0); private set

    /** Keyed by provider name. */
    val feeds = mutableStateMapOf<String, ProviderFeed>()
    private val feedFetchedAt = HashMap<String, Long>()

    var resumed = false
    private var lastActivity = SystemClock.elapsedRealtime()
    private var weatherJob: Job? = null

    val dockApps: List<AppEntry>
        get() = pinned.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }

    init {
        if (autoStart) start()
    }

    private fun start() {
        ambient = true // the TV wakes up on the clock screen; any key reveals the home screen
        refreshApps()
        startWeather()
        viewModelScope.launch {
            while (true) {
                val url = withContext(Dispatchers.IO) {
                    runCatching { DailyPhoto.url(Locale.getDefault().toLanguageTag()) }
                        .onFailure { Log.w(TAG, "photo failed", it) }
                        .getOrNull()
                }
                if (url != null) photoUrl = url
                delay(if (url == null) 15 * 60_000L else 6 * 3600_000L)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(10_000)
                val minutes = prefs.ambientMinutes
                val idle = SystemClock.elapsedRealtime() - lastActivity
                if (minutes > 0 && resumed && !ambient && screen == Screen.Home && idle > minutes * 60_000L) {
                    ambient = true
                }
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { AppRepository.load(getApplication()) }
            apps = list
            if (!prefs.hasPinned) prefs.pinned = AppRepository.defaultPins(list)
            pinned = prefs.pinned.filter { pkg -> list.any { it.packageName == pkg } }
        }
    }

    private val language: String
        get() = Locale.getDefault().toLanguageTag()

    fun requestFeed(app: AppEntry) {
        val provider = Providers.match(app) ?: return
        val key = prefs.tmdbKey
        if (key.isBlank()) return
        val last = feedFetchedAt[provider.name]
        if (last != null && SystemClock.elapsedRealtime() - last < 6 * 3600_000L) return
        feedFetchedAt[provider.name] = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            val feed = withContext(Dispatchers.IO) {
                runCatching { Tmdb.feed(key, provider, prefs.region, language) }
                    .onFailure { Log.w(TAG, "TMDB feed failed for ${provider.name}", it) }
                    .getOrNull()
            }
            if (feed != null) feeds[provider.name] = feed
            else feedFetchedAt[provider.name] = SystemClock.elapsedRealtime() - 5 * 3600_000L // retry in ~1h
        }
    }

    fun feedFor(app: AppEntry): ProviderFeed? = Providers.match(app)?.let { feeds[it.name] }

    private fun startWeather() {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            while (true) {
                val w = withContext(Dispatchers.IO) {
                    runCatching { WeatherApi.load(prefs.city, language) }
                        .onFailure { Log.w(TAG, "weather failed", it) }
                        .getOrNull()
                }
                if (w != null || prefs.city.isBlank()) weather = w
                delay(if (w == null && prefs.city.isNotBlank()) 5 * 60_000L else 30 * 60_000L)
            }
        }
    }

    fun launch(context: Context, app: AppEntry) {
        runCatching { context.startActivity(app.intent) }
            .onFailure { Toast.makeText(context, it.message ?: "Error", Toast.LENGTH_SHORT).show() }
    }

    fun isPinned(pkg: String) = pkg in pinned

    fun togglePin(pkg: String): Boolean {
        val now = if (pkg in pinned) pinned - pkg else pinned + pkg
        pinned = now
        prefs.pinned = now
        return pkg in now
    }

    fun movePin(pkg: String, delta: Int) {
        val list = pinned.toMutableList()
        val i = list.indexOf(pkg)
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        list.removeAt(i)
        list.add(j, pkg)
        pinned = list
        prefs.pinned = list
    }

    fun updateSettings(
        city: String? = null,
        region: String? = null,
        tmdbKey: String? = null,
        ambientMinutes: Int? = null,
    ) {
        city?.let { prefs.city = it; startWeather() }
        region?.let { prefs.region = it; feeds.clear(); feedFetchedAt.clear() }
        tmdbKey?.let { prefs.tmdbKeyOverride = it; feeds.clear(); feedFetchedAt.clear() }
        ambientMinutes?.let { prefs.ambientMinutes = it }
        settingsVersion++
    }

    /** Returns true if the key press was used to wake from the ambient screen. */
    fun onUserActivity(): Boolean {
        lastActivity = SystemClock.elapsedRealtime()
        if (ambient) {
            ambient = false
            return true
        }
        return false
    }

    private companion object {
        const val TAG = "TvLauncher"
    }
}
