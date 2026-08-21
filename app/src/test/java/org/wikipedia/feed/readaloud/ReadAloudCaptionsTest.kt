package org.wikipedia.feed.readaloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadAloudCaptionsTest {

    @Test
    fun testParseWordCues() {
        val cues = ReadAloudCaptions.parse(
            """
            WEBVTT

            00:00:00.120 --> 00:00:00.800
            Palladian

            00:00:00.820 --> 00:00:01.380
            architecture.

            00:01:02.617 --> 00:01:03.297
            Andrea
            """.trimIndent()
        )

        assertEquals(3, cues.size)
        assertEquals(ReadAloudCue(120, 800, "Palladian"), cues[0])
        assertEquals(ReadAloudCue(820, 1380, "architecture."), cues[1])
        assertEquals(ReadAloudCue(62617, 63297, "Andrea"), cues[2])
    }

    @Test
    fun testParseSkipsNonCueBlocks() {
        val cues = ReadAloudCaptions.parse(
            """
            WEBVTT - with a trailing header comment

            NOTE
            This block is not a cue.

            cue-identifier-1
            00:00:01.000 --> 00:00:02.000 align:start position:10%
            hello

            00:02.500 --> 00:03.000
            world
            """.trimIndent()
        )

        assertEquals(2, cues.size)
        assertEquals(ReadAloudCue(1000, 2000, "hello"), cues[0])
        // A two-part timestamp omits the hour.
        assertEquals(ReadAloudCue(2500, 3000, "world"), cues[1])
    }

    @Test
    fun testParseJoinsMultiLinePayloadAndSkipsMalformedCues() {
        val cues = ReadAloudCaptions.parse(
            """
            WEBVTT

            00:00:01.000 --> 00:00:02.000
            two
            lines

            not-a-timestamp --> also-not-one
            ignored

            00:00:03.000 --> 00:00:04.000
            """.trimIndent()
        )

        assertEquals(1, cues.size)
        assertEquals(ReadAloudCue(1000, 2000, "two lines"), cues[0])
    }

    @Test
    fun testParseEmptyInput() {
        assertTrue(ReadAloudCaptions.parse("").isEmpty())
        assertTrue(ReadAloudCaptions.parse("WEBVTT").isEmpty())
    }
}
