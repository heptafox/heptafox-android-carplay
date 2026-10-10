package com.heptafox.carplay.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Finds a Spotify track id for a query with Google's "I'm Feeling Lucky" redirect, scoped to
 * open.spotify.com/track. No key: Spotify's search API needs Premium and Google's Custom Search
 * API is closed to new projects.
 */
class SpotifySearch {

    suspend fun firstTrackId(query: String): String? = withContext(Dispatchers.IO) {
        val url = URL(
            "https://www.google.com/search?btnI=1" +
                "&q=${URLEncoder.encode("site:open.spotify.com/track $query", Charsets.UTF_8)}",
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val status = connection.responseCode
            // ponytail: no result redirects back to a results page; a bot block gives a /sorry/ page.
            // Both end up as "no results"; if Google starts blocking for real, swap the source here.
            if (status / 100 != 3) throw PlaybackException("Spotify lookup failed (HTTP $status)")

            connection.getHeaderField("Location")?.let { TRACK_URL.find(it)?.groupValues?.get(1) }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 8_000
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130 Mobile Safari/537.36"
        val TRACK_URL = Regex("""open\.spotify\.com/track/([A-Za-z0-9]{22})""")
    }
}
