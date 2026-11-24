package com.beatriz.tvlauncher.data

/** Daily landscape photo for the ambient screen (Bing "photo of the day", no key). */
object DailyPhoto {
    data class Photo(val url: String, val caption: String)

    fun today(market: String): Photo? {
        val img = Http.getJson("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=" + Http.enc(market))
            .optJSONArray("images")?.optJSONObject(0) ?: return null
        val path = img.optString("url").takeIf { it.startsWith("/") } ?: return null
        // "Broto de samambaia (© Photographer/Agency)" -> "Broto de samambaia"
        val caption = img.optString("copyright").substringBefore(" (©").substringBefore("(©").trim()
        return Photo("https://www.bing.com$path", caption)
    }
}
