package org.wikipedia.feed.readaloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.util.log.L
import kotlin.math.roundToLong

private const val TIMESTAMP_SEPARATOR = "-->"

/**
 * A single spoken word, and the span of the recording during which it is spoken.
 */
data class ReadAloudCue(
    val startMillis: Long,
    val endMillis: Long,
    val word: String
)

/**
 * The word-level captions that accompany a lead-section recording, served as a WebVTT file next to
 * the .mp3. The service emits exactly one word per cue, which is what lets the card highlight the
 * narration word by word.
 */
object ReadAloudCaptions {

    suspend fun fetch(url: String): List<ReadAloudCue> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            OkHttpConnectionFactory.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) parse(response.body.string()) else emptyList()
            }
        } catch (e: Exception) {
            L.e(e)
            emptyList()
        }
    }

    /**
     * Reads the cues out of a WebVTT file. Anything that isn't a cue — the `WEBVTT` header, `NOTE`
     * blocks, cue identifiers, cue settings trailing the end timestamp — is skipped rather than
     * treated as an error, so a slightly different file from the service still plays along.
     */
    fun parse(webVtt: String): List<ReadAloudCue> {
        val cues = mutableListOf<ReadAloudCue>()
        val lines = webVtt.lines()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            index++
            val separatorIndex = line.indexOf(TIMESTAMP_SEPARATOR)
            if (separatorIndex < 0) {
                continue
            }
            val startMillis = parseTimestamp(line.substring(0, separatorIndex))
            val endMillis = parseTimestamp(line.substring(separatorIndex + TIMESTAMP_SEPARATOR.length).trim().substringBefore(' '))
            if (startMillis == null || endMillis == null) {
                continue
            }
            val word = buildString {
                while (index < lines.size && lines[index].isNotBlank()) {
                    if (isNotEmpty()) {
                        append(' ')
                    }
                    append(lines[index].trim())
                    index++
                }
            }
            if (word.isNotEmpty()) {
                cues.add(ReadAloudCue(startMillis, endMillis, word))
            }
        }
        return cues
    }

    /**
     * Parses a `HH:MM:SS.mmm` or `MM:SS.mmm` WebVTT timestamp into milliseconds.
     */
    private fun parseTimestamp(text: String): Long? {
        val parts = text.trim().split(':')
        if (parts.size !in 2..3) {
            return null
        }
        val seconds = parts.last().replace(',', '.').toDoubleOrNull() ?: return null
        val minutes = parts[parts.size - 2].toLongOrNull() ?: return null
        val hours = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
        return (hours * 3600 + minutes * 60) * 1000 + (seconds * 1000).roundToLong()
    }
}
