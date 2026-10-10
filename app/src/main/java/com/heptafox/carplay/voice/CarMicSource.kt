package com.heptafox.carplay.voice

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.media.CarAudioRecord
import java.io.Closeable
import kotlin.concurrent.thread

/**
 * Streams the car's microphone into a pipe whose read end ([audio]) can be handed to a speech recognizer.
 *
 * While open it holds exclusive transient audio focus, so the user's music pauses and isn't recorded.
 * Create with [open]; always [close] when done.
 */
class CarMicSource private constructor(
    private val record: CarAudioRecord,
    private val audioManager: AudioManager,
) : Closeable {

    private val pipe = ParcelFileDescriptor.createPipe()

    /** Raw PCM: [ENCODING], [CHANNELS] channel, [SAMPLE_RATE] Hz. */
    val audio: ParcelFileDescriptor = pipe[0]
    private val sink: ParcelFileDescriptor = pipe[1]

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .build(),
        )
        .setOnAudioFocusChangeListener { change -> if (change < 0) close() } // any kind of focus loss
        .build()

    @Volatile
    private var recording = false

    private fun start(): Boolean {
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
        recording = true
        thread(name = "car-mic") { pump() }
        return true
    }

    private fun pump() {
        val buffer = ByteArray(CarAudioRecord.AUDIO_CONTENT_BUFFER_SIZE)
        try {
            record.startRecording()
            ParcelFileDescriptor.AutoCloseOutputStream(sink).use { out ->
                while (recording) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read < 0) break // the host closed the mic
                    out.write(buffer, 0, read)
                }
            }
        } catch (e: Exception) {
            // Usually the recognizer finished and closed its end of the pipe; the reader sees EOF either way.
            Log.d(TAG, "Car mic stopped: $e")
        } finally {
            sink.close()
            runCatching { record.stopRecording() }
        }
    }

    override fun close() {
        recording = false
        audio.close()
        sink.close() // unblocks a pending write in pump()
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    companion object {
        private const val TAG = "CarMicSource"

        const val SAMPLE_RATE = CarAudioRecord.AUDIO_CONTENT_SAMPLING_RATE
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val CHANNELS = 1

        /** Returns null without the RECORD_AUDIO permission or when audio focus is refused (e.g. during a call). */
        fun open(carContext: CarContext): CarMicSource? {
            if (carContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return null
            }
            val source = CarMicSource(
                CarAudioRecord.create(carContext),
                checkNotNull(carContext.getSystemService(AudioManager::class.java)),
            )
            if (source.start()) return source
            source.close()
            return null
        }
    }
}
