package com.beatriz.tvlauncher.data

import org.json.JSONObject

data class Title(
    val id: Int,
    val type: String, // "tv" or "movie"
    val name: String,
    val overview: String,
    val backdrop: String?,
    val popularity: Double,
)

data class ProviderFeed(
    val provider: Provider,
    val region: String,
    val hero: Title,
    val logoPath: String?,
    val seasons: Int?,
    val episodes: Int?,
    val runtimeMin: Int?,
    val more: List<Title>,
)

/** The Movie Database (themoviedb.org): free API key, "where to watch" data by country. */
object Tmdb {
    private const val API = "https://api.themoviedb.org/3"
    private const val IMG = "https://image.tmdb.org/t/p/"

    fun image(path: String?, size: String = "w1280"): String? = path?.let { IMG + size + it }

    private fun get(key: String, path: String, params: Map<String, String>): JSONObject {
        // v4 "read access tokens" are long JWTs sent as Bearer; v3 keys are 32-char query params.
        val bearer = key.length > 40
        val q = if (bearer) params else params + ("api_key" to key)
        val headers = if (bearer) mapOf("Authorization" to "Bearer $key") else emptyMap()
        return Http.getJson("$API$path?${Http.query(q)}", headers)
    }

    fun feed(key: String, provider: Provider, region: String, language: String): ProviderFeed? {
        val base = mapOf(
            "watch_region" to region,
            "with_watch_providers" to provider.tmdbIds,
            "with_watch_monetization_types" to "flatrate",
            "sort_by" to "popularity.desc",
            "include_adult" to "false",
            "language" to language,
        )
        val titles = ArrayList<Title>()
        for (type in listOf("tv", "movie")) {
            val results = get(key, "/discover/$type", base).optJSONArray("results") ?: continue
            for (i in 0 until results.length()) {
                val o = results.getJSONObject(i)
                val backdrop = o.optString("backdrop_path").takeIf { it.startsWith("/") } ?: continue
                titles += Title(
                    id = o.getInt("id"),
                    type = type,
                    name = o.optString(if (type == "tv") "name" else "title"),
                    overview = o.optString("overview"),
                    backdrop = backdrop,
                    popularity = o.optDouble("popularity", 0.0),
                )
            }
        }
        if (titles.isEmpty()) return null
        val sorted = titles.sortedByDescending { it.popularity }.take(14)
        val hero = sorted.first()

        val lang2 = language.substringBefore('-')
        val detail = runCatching {
            get(
                key, "/${hero.type}/${hero.id}",
                mapOf(
                    "language" to language,
                    "append_to_response" to "images",
                    "include_image_language" to "$lang2,en,null",
                ),
            )
        }.getOrNull()

        val logos = detail?.optJSONObject("images")?.optJSONArray("logos")
        var logo: String? = null
        if (logos != null && logos.length() > 0) {
            val list = (0 until logos.length()).map { logos.getJSONObject(it) }
            val best = list.firstOrNull { it.optString("iso_639_1") == lang2 }
                ?: list.firstOrNull { it.optString("iso_639_1") == "en" }
                ?: list.first()
            logo = best.optString("file_path").takeIf { it.startsWith("/") }
        }

        return ProviderFeed(
            provider = provider,
            region = region,
            hero = hero.copy(overview = detail?.optString("overview")?.ifBlank { null } ?: hero.overview),
            logoPath = logo,
            seasons = detail?.optInt("number_of_seasons")?.takeIf { it > 0 },
            episodes = detail?.optInt("number_of_episodes")?.takeIf { it > 0 },
            runtimeMin = detail?.optInt("runtime")?.takeIf { it > 0 },
            more = sorted.drop(1),
        )
    }
}
