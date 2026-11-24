package com.beatriz.tvlauncher.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Http {
    fun getJson(url: String, headers: Map<String, String> = emptyMap()): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Accept", "application/json")
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code for ${url.substringBefore('?')}")
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    fun query(params: Map<String, String>): String =
        params.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
}
