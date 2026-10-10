package com.heptafox.carplay.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.heptafox.carplay.assistant.AssistantState
import com.heptafox.carplay.assistant.VoiceAssistant
import com.heptafox.carplay.setup.Permissions
import com.heptafox.carplay.voice.SpeechListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shows "Listening…" while one request runs, then pops itself with the [AssistantState] as its result.
 * Pressing back cancels the request (the coroutine dies with the screen's lifecycle).
 */
class ListeningScreen(
    carContext: CarContext,
    private val speech: SpeechListener,
    private val assistant: VoiceAssistant,
) : Screen(carContext) {

    init {
        // Dispatchers.Main (not Main.immediate) defers the work until push() has finished, so an
        // instant result (e.g. a missing permission) pops this screen rather than the home screen.
        lifecycleScope.launch(Dispatchers.Main) {
            finishWith(listenAndPlay())
        }
    }

    private suspend fun listenAndPlay(): AssistantState =
        if (Permissions.allGranted(carContext)) {
            assistant.handle(speech.listen())
        } else {
            AssistantState.MissingPermissions
        }

    private fun finishWith(result: AssistantState) {
        setResult(result)
        screenManager.pop()
    }

    override fun onGetTemplate(): Template =
        MessageTemplate.Builder("Listening…")
            .setHeader(
                Header.Builder()
                    .setTitle("Hepta")
                    .setStartHeaderAction(Action.BACK)
                    .build(),
            )
            .setLoading(true)
            .build()
}
