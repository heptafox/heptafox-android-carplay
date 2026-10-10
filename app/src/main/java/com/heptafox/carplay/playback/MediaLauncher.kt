package com.heptafox.carplay.playback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.heptafox.carplay.BuildConfig
import com.heptafox.carplay.command.PlayCommand
import com.heptafox.carplay.command.Provider

/** Starts playback of a search query in one specific app. */
interface MediaLauncher {
    /** @throws PlaybackException when the query can't be played. */
    suspend fun launch(query: String)
}

/** A failure with a message that is safe to show on the car screen. */
class PlaybackException(message: String) : Exception(message)

/** Routes a [PlayCommand] to the launcher for its [Provider]. */
class MediaLaunchers(
    private val spotify: MediaLauncher,
    private val youtube: MediaLauncher,
) {
    suspend fun launch(command: PlayCommand) {
        val launcher = when (command.provider) {
            Provider.SPOTIFY -> spotify
            Provider.YOUTUBE -> youtube
        }
        launcher.launch(command.query)
    }

    companion object {
        fun create(context: Context): MediaLaunchers {
            val app = context.applicationContext
            return MediaLaunchers(
                spotify = SpotifyLauncher(app, SpotifySearch()),
                youtube = YouTubeLauncher(app, YouTubeSearch(BuildConfig.YT_API_KEY)),
            )
        }
    }
}

internal fun Context.startAppActivity(intent: Intent, appName: String) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        throw PlaybackException("$appName isn't installed")
    }
}
