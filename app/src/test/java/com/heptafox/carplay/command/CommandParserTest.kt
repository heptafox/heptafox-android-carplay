package com.heptafox.carplay.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommandParserTest {

    @Test
    fun `plain play uses the default provider`() {
        assertEquals(
            PlayCommand(CommandParser.DEFAULT_PROVIDER, "believer"),
            CommandParser.parse("Play Believer"),
        )
    }

    @Test
    fun `on youtube picks youtube and is removed from the query`() {
        assertEquals(
            PlayCommand(Provider.YOUTUBE, "cricket highlights"),
            CommandParser.parse("play cricket highlights on YouTube"),
        )
    }

    @Test
    fun `provider can come before the verb`() {
        assertEquals(
            PlayCommand(Provider.SPOTIFY, "arijit singh"),
            CommandParser.parse("Spotify play Arijit Singh"),
        )
    }

    @Test
    fun `recognizer spelling you tube still means youtube`() {
        assertEquals(
            PlayCommand(Provider.YOUTUBE, "lofi beats"),
            CommandParser.parse("play lofi beats on you tube"),
        )
    }

    @Test
    fun `search for is a verb`() {
        assertEquals(
            PlayCommand(Provider.YOUTUBE, "tamil podcasts"),
            CommandParser.parse("search for tamil podcasts in youtube"),
        )
    }

    @Test
    fun `extra whitespace is collapsed`() {
        assertEquals(
            PlayCommand(Provider.SPOTIFY, "shape of you"),
            CommandParser.parse("  play   shape  of you on   spotify "),
        )
    }

    @Test
    fun `phrase without a verb is still a query`() {
        assertEquals(PlayCommand(CommandParser.DEFAULT_PROVIDER, "believer"), CommandParser.parse("believer"))
    }

    @Test
    fun `verb words inside a title are kept`() {
        assertEquals(
            PlayCommand(CommandParser.DEFAULT_PROVIDER, "start me up"),
            CommandParser.parse("play start me up"),
        )
    }

    @Test
    fun `nothing to search is not understood`() {
        assertNull(CommandParser.parse("play"))
        assertNull(CommandParser.parse("open youtube"))
        assertNull(CommandParser.parse("on spotify"))
        assertNull(CommandParser.parse("   "))
    }
}
