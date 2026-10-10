package com.heptafox.carplay.playback

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.provider.MediaStore

/**
 * Asks Spotify to search and play via the standard "play from search" intent.
 * Works on the free tier: Spotify's own APIs require Premium for developer apps.
 */
class SpotifyLauncher(private val context: Context) : MediaLauncher {

    override suspend fun launch(query: String) {
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            .setPackage(SPOTIFY_PACKAGE)
            .putExtra(SearchManager.QUERY, query)
            .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, UNSTRUCTURED_FOCUS)
        context.startAppActivity(intent, "Spotify")
    }

    private companion object {
        const val SPOTIFY_PACKAGE = "com.spotify.music"

        /** "Any kind of media": lets Spotify decide between track, artist, playlist or podcast. */
        const val UNSTRUCTURED_FOCUS = "vnd.android.cursor.item/*"
    }
}
