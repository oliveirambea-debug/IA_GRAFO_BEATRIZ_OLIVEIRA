package com.beatriz.tvlauncher.data

import android.content.Context
import com.beatriz.tvlauncher.BuildConfig
import java.util.Locale

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    val hasPinned: Boolean get() = sp.contains(KEY_PINNED)

    var pinned: List<String>
        get() = sp.getString(KEY_PINNED, "")!!.split(',').filter { it.isNotBlank() }
        set(value) = sp.edit().putString(KEY_PINNED, value.joinToString(",")).apply()

    var city: String
        get() = sp.getString("city", "")!!
        set(value) = sp.edit().putString("city", value.trim()).apply()

    /** Key typed in Settings; falls back to the one baked in at build time. */
    var tmdbKeyOverride: String
        get() = sp.getString("tmdb", "")!!
        set(value) = sp.edit().putString("tmdb", value.trim()).apply()

    val tmdbKey: String get() = tmdbKeyOverride.ifBlank { BuildConfig.TMDB_API_KEY }

    var region: String
        get() = sp.getString("region", null) ?: Locale.getDefault().country.ifBlank { "US" }
        set(value) = sp.edit().putString("region", value.trim().uppercase()).apply()

    /** 0 = never. */
    var ambientMinutes: Int
        get() = sp.getInt("ambient", 5)
        set(value) = sp.edit().putInt("ambient", value).apply()

    private companion object {
        const val KEY_PINNED = "pinned"
    }
}
