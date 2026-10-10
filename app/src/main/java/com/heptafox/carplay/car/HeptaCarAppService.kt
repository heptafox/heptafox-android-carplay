package com.heptafox.carplay.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.heptafox.carplay.assistant.VoiceAssistant
import com.heptafox.carplay.playback.MediaLaunchers
import com.heptafox.carplay.voice.SpeechListener

/** Entry point Android Auto binds to. Wires the app's pieces together for each car session. */
class HeptaCarAppService : CarAppService() {

    // Sideloaded personal app, so any host may connect. Restrict to Google's hosts before publishing.
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen = VoiceScreen(
            carContext = carContext,
            speech = SpeechListener(carContext),
            assistant = VoiceAssistant(MediaLaunchers.create(carContext)),
        )
    }
}
