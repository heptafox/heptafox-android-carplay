package com.heptafox.carplay.assistant

import android.util.Log
import com.heptafox.carplay.command.CommandParser
import com.heptafox.carplay.playback.MediaLaunchers
import com.heptafox.carplay.playback.PlaybackException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Turns what the user said into playback. Knows nothing about screens or microphones. */
class VoiceAssistant(private val launchers: MediaLaunchers) {

    /** @param spoken the transcript, or null when the recognizer heard nothing. */
    suspend fun handle(spoken: String?): AssistantState {
        if (spoken.isNullOrBlank()) return AssistantState.NotHeard
        val command = CommandParser.parse(spoken) ?: return AssistantState.NotUnderstood(spoken)
        return try {
            launchers.launch(command)
            AssistantState.Playing(command)
        } catch (e: PlaybackException) {
            AssistantState.Failed(e.message.orEmpty())
        } catch (e: IOException) {
            Log.w(TAG, "Network error for $command", e)
            AssistantState.Failed("network error")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // An uncaught exception here kills Android Auto's projection process too, not just Hepta.
            Log.e(TAG, "Unexpected failure for $command", e)
            AssistantState.Failed("something went wrong")
        }
    }

    private companion object {
        const val TAG = "VoiceAssistant"
    }
}
