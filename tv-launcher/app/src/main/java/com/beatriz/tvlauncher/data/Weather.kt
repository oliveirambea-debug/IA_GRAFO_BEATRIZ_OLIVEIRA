package com.beatriz.tvlauncher.data

import org.json.JSONObject
import java.time.LocalDateTime

data class Weather(
    val city: String,
    val tempC: Int,
    val code: Int,
    /** Minutes after local midnight. */
    val sunriseMin: Int,
    val sunsetMin: Int,
)

/** Open-Meteo: free, no API key. */
object WeatherApi {
    fun load(city: String, language: String): Weather? {
        if (city.isBlank()) return null
        val geo = Http.getJson(
            "https://geocoding-api.open-meteo.com/v1/search?" +
                Http.query(mapOf("name" to city, "count" to "1", "language" to language.substringBefore('-')))
        ).optJSONArray("results")?.optJSONObject(0) ?: return null

        val f = Http.getJson(
            "https://api.open-meteo.com/v1/forecast?" + Http.query(
                mapOf(
                    "latitude" to geo.getDouble("latitude").toString(),
                    "longitude" to geo.getDouble("longitude").toString(),
                    "current" to "temperature_2m,weather_code",
                    "daily" to "sunrise,sunset",
                    "timezone" to "auto",
                    "forecast_days" to "1",
                )
            )
        )
        val current = f.getJSONObject("current")
        val daily = f.getJSONObject("daily")
        return Weather(
            city = geo.optString("name", city),
            tempC = Math.round(current.getDouble("temperature_2m")).toInt(),
            code = current.getInt("weather_code"),
            sunriseMin = minutes(daily, "sunrise") ?: (6 * 60),
            sunsetMin = minutes(daily, "sunset") ?: (18 * 60),
        )
    }

    private fun minutes(daily: JSONObject, field: String): Int? = runCatching {
        val t = LocalDateTime.parse(daily.getJSONArray(field).getString(0))
        t.hour * 60 + t.minute
    }.getOrNull()
}
