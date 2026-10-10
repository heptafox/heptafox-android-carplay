package com.heptafox.carplay.command

/**
 * Turns a spoken phrase such as "play believer on spotify" into a [PlayCommand].
 *
 * Rules: naming "YouTube" or "Spotify" anywhere picks that app (otherwise [DEFAULT_PROVIDER]);
 * the app name and a leading verb ("play", "search for", ...) are removed; whatever remains is the search query.
 */
object CommandParser {

    val DEFAULT_PROVIDER = Provider.SPOTIFY

    private val whitespace = Regex("""\s+""")
    private val providerMention = Regex("""\b(?:(?:on|in|from|using)\s+)?(you\s?tube|spotify)\b""")
    private val leadingVerb =
        Regex("""^(?:please\s+)?(?:play|search for|search|put on|listen to|open)(?:\s+|$)""")

    fun parse(spoken: String): PlayCommand? {
        val text = spoken.lowercase().normalizeSpaces()
        val mention = providerMention.find(text)?.groupValues?.get(1)
        val provider = when {
            mention == null -> DEFAULT_PROVIDER
            mention == "spotify" -> Provider.SPOTIFY
            else -> Provider.YOUTUBE
        }
        val query = text
            .replace(providerMention, " ")
            .normalizeSpaces()
            .replace(leadingVerb, "")
            .trim()
        return if (query.isEmpty()) null else PlayCommand(provider, query)
    }

    private fun String.normalizeSpaces() = replace(whitespace, " ").trim()
}
