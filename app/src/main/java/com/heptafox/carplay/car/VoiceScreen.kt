package com.heptafox.carplay.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import com.heptafox.carplay.assistant.AssistantState
import com.heptafox.carplay.assistant.VoiceAssistant
import com.heptafox.carplay.voice.SpeechListener

/**
 * Home screen: the last result plus a "Speak" button.
 *
 * Android Auto allows only 5 template changes per task, so this screen never changes its own text.
 * Each request runs on a pushed [ListeningScreen]; its result comes back via a pop, and content
 * changes on a pop are free. That lets the driver speak as often as they like.
 */
class VoiceScreen(
    carContext: CarContext,
    private val speech: SpeechListener,
    private val assistant: VoiceAssistant,
) : Screen(carContext) {

    private var state: AssistantState = AssistantState.Idle

    private val speakAction = Action.Builder()
        .setTitle("Speak")
        .setFlags(Action.FLAG_PRIMARY)
        .setOnClickListener(::startListening)
        .build()

    override fun onGetTemplate(): Template =
        MessageTemplate.Builder(state.message)
            .setHeader(
                Header.Builder()
                    .setTitle("Hepta")
                    .setStartHeaderAction(Action.APP_ICON)
                    .build(),
            )
            .addAction(speakAction)
            .build()

    private fun startListening() {
        screenManager.pushForResult(ListeningScreen(carContext, speech, assistant)) { result ->
            // No invalidate(): the host asks for this screen's template right after the pop,
            // and that request arrives after this result is stored.
            if (result is AssistantState) state = result
        }
    }
}
