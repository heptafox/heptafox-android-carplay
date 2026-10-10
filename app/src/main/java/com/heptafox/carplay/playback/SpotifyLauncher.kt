package com.heptafox.carplay.playback

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens a track URI in Spotify, which autoplays on the free tier. The obvious routes don't work:
 * MEDIA_PLAY_FROM_SEARCH only fills in Spotify's search box since 9.x, and Spotify's media session
 * and MediaBrowserService ignore playFromSearch from any app that isn't Assistant or Android Auto.
 */
class SpotifyLauncher(
    private val context: Context,
    private val search: SpotifySearch,
) : MediaLauncher {

    override suspend fun launch(query: String) {
        val trackId = search.firstTrackId(query) ?: throw PlaybackException("No Spotify results for \"$query\"")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:track:$trackId"))
            .setPackage(SPOTIFY_PACKAGE)
        context.startAppActivity(intent, "Spotify")
    }

    private companion object {
        const val SPOTIFY_PACKAGE = "com.spotify.music"
    }
}
