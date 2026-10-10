package com.heptafox.carplay.command

enum class Provider(val displayName: String) {
    SPOTIFY("Spotify"),
    YOUTUBE("YouTube"),
}

/** What the user asked for: search [query] and play the top result on [provider]. */
data class PlayCommand(val provider: Provider, val query: String)
