package com.heptafox.carplay.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Finds the top video for a query with the YouTube Data API v3 (each search costs 100 of the 10,000 free daily units). */
class YouTubeSearch(private val apiKey: String) {

    suspend fun firstVideoId(query: String): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw PlaybackException("YouTube API key missing: set YT_API_KEY in local.properties")

        val url = URL(
            "https://www.googleapis.com/youtube/v3/search" +
                "?part=id&type=video&maxResults=1" +
                "&q=${URLEncoder.encode(query, Charsets.UTF_8)}" +
                "&key=$apiKey",
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) throw PlaybackException("YouTube search failed (HTTP $status)")

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseFirstVideoId(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseFirstVideoId(body: String): String? = try {
        JSONObject(body)
            .getJSONArray("items")
            .optJSONObject(0)
            ?.optJSONObject("id")
            ?.optString("videoId")
            ?.takeIf { it.isNotEmpty() }
    } catch (e: JSONException) {
        throw PlaybackException("unexpected response from YouTube")
    }

    private companion object {
        const val TIMEOUT_MS = 8_000
    }
}
