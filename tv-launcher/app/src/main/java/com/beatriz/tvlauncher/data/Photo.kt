package com.beatriz.tvlauncher.data

/** Daily landscape photo for the ambient screen (Bing "photo of the day", no key). */
object DailyPhoto {
    fun url(market: String): String? {
        val json = Http.getJson("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=" + Http.enc(market))
        val path = json.optJSONArray("images")?.optJSONObject(0)?.optString("url")?.takeIf { it.startsWith("/") } ?: return null
        return "https://www.bing.com$path"
    }
}
