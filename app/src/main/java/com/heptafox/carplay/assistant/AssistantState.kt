package com.heptafox.carplay.assistant

import com.heptafox.carplay.command.PlayCommand

/** Outcome of one voice request, with the text to show the driver. */
sealed interface AssistantState {
    val message: String

    data object Idle : AssistantState {
        override val message = "Tap Speak and say \"play <song> on Spotify\" or \"play <topic> on YouTube\"."
    }

    data class Playing(val command: PlayCommand) : AssistantState {
        override val message get() = "Playing \"${command.query}\" on ${command.provider.displayName}."
    }

    data object NotHeard : AssistantState {
        override val message = "Didn't catch that. Tap Speak to try again."
    }

    data class NotUnderstood(val heard: String) : AssistantState {
        override val message get() = "Heard \"$heard\" but there's nothing to search. Try \"play <song> on YouTube\"."
    }

    data object MissingPermissions : AssistantState {
        override val message = "Open Hepta on your phone and allow the microphone and \"Display over other apps\"."
    }

    data class Failed(val reason: String) : AssistantState {
        override val message get() = "Couldn't play: $reason."
    }
}
