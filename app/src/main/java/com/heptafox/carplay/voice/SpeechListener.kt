package com.heptafox.carplay.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.car.app.CarContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Listens for one utterance on the car microphone using Android's speech recognizer.
 *
 * Must be called on the main thread.
 */
class SpeechListener(private val carContext: CarContext) {

    /** Returns the best transcript, or null if nothing was understood. */
    suspend fun listen(): String? {
        if (!SpeechRecognizer.isRecognitionAvailable(carContext)) {
            Log.w(TAG, "No speech recognizer on this phone")
            return null
        }
        val mic = CarMicSource.open(carContext) ?: return null
        val recognizer = SpeechRecognizer.createSpeechRecognizer(carContext)
        try {
            return withTimeoutOrNull(MAX_LISTEN_MS + STOP_GRACE_MS) {
                coroutineScope {
                    // In case the recognizer never hears the end of speech (e.g. constant road noise), cut it
                    // off; stopping makes it hand over what it heard so far.
                    val cutoff = launch {
                        delay(MAX_LISTEN_MS)
                        recognizer.stopListening()
                    }
                    suspendCancellableCoroutine { continuation ->
                        recognizer.setRecognitionListener(
                            ResultListener(onSpeechEnded = recognizer::stopListening) { transcript ->
                                if (continuation.isActive) continuation.resume(transcript)
                            },
                        )
                        recognizer.startListening(recognizerIntent(mic))
                    }.also { cutoff.cancel() }
                }
            }
        } finally {
            recognizer.destroy()
            mic.close()
        }
    }

    private fun recognizerIntent(mic: CarMicSource) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, mic.audio)
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, CarMicSource.ENCODING)
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, CarMicSource.CHANNELS)
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, CarMicSource.SAMPLE_RATE)

    /**
     * Reports the final transcript (or null on error) once.
     *
     * Fed our own audio stream, Google's on-device recognizer only puts the transcript in partial results and
     * never finishes by itself: it reports end of speech, and a forced stop then returns empty results.
     * So the end of speech triggers [onSpeechEnded] (a stop), and the last partial stands in for empty results.
     */
    private class ResultListener(
        private val onSpeechEnded: () -> Unit,
        private val onTranscript: (String?) -> Unit,
    ) : RecognitionListener {
        private var lastPartial: String? = null

        override fun onResults(results: Bundle?) = onTranscript(results.bestTranscript() ?: lastPartial)

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults.bestTranscript()?.let { lastPartial = it }
        }

        override fun onEndOfSpeech() = onSpeechEnded()

        override fun onError(error: Int) {
            Log.d(TAG, "Recognition error $error")
            onTranscript(null)
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        private fun Bundle?.bestTranscript(): String? =
            this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val TAG = "SpeechListener"

        /** Longest a request listens before transcribing what it has. */
        const val MAX_LISTEN_MS = 8_000L

        /** How long to wait for the transcript after cutting off, before giving up. */
        const val STOP_GRACE_MS = 3_000L
    }
}
