package com.heptafox.carplay.playback

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the top search result in the YouTube app. Android Auto won't show the video, but the
 * audio plays through the car. It keeps playing on a locked phone only with YouTube Premium.
 */
class YouTubeLauncher(
    private val context: Context,
    private val search: YouTubeSearch,
) : MediaLauncher {

    override suspend fun launch(query: String) {
        val videoId = search.firstVideoId(query) ?: throw PlaybackException("No YouTube results for \"$query\"")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
            .setPackage(YOUTUBE_PACKAGE)
        context.startAppActivity(intent, "YouTube")
    }

    private companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
