package org.wikipedia.feed.readaloud

import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

/**
 * Provides the hardcoded set of articles whose lead section has a pre-generated audio version,
 * from the bundled `ttsarticles.csv` asset.
 */
object ReadAloudArticlesRepository {
    private const val ASSET_FILE_NAME = "ttsarticles.csv"
    private const val LANGUAGE_CODE = "en"
    private const val AUDIO_BASE_URL = "https://wiki-tts.toolforge.org/audio/"
    private const val LEAD_SECTION_NAME = "Lead"

    fun isSupported(wikiSite: WikiSite) = wikiSite.languageCode == LANGUAGE_CODE

    fun audioUrlFor(title: PageTitle) = mediaUrlFor(title, "mp3")

    fun captionsUrlFor(title: PageTitle) = mediaUrlFor(title, "vtt")

    private fun mediaUrlFor(title: PageTitle, extension: String) =
        AUDIO_BASE_URL + UriUtil.encodeURL(title.prefixedText) + "/$LEAD_SECTION_NAME.$extension"

    /**
     * Returns up to [count] distinct random articles belonging to the given interest topic, or an
     * empty list if the topic has no articles with an audio version.
     */
    fun randomArticlesForTopic(wikiSite: WikiSite, topicId: String, count: Int): List<PageTitle> {
        return readArticleTitlesByTopic()[queryTopicIdOf(topicId)].orEmpty()
            .shuffled()
            .take(count)
            .map { PageTitle(it, wikiSite) }
    }

    // The topic column of the CSV holds the topic ids used by the search API, which differ from our
    // own topic ids for a handful of topics (e.g. "literature" vs "books").
    private fun queryTopicIdOf(topicId: String): String {
        return ArticleTopics.all.find { it.topicId == topicId }?.queryTopicId ?: topicId
    }

    private fun readArticleTitlesByTopic(): Map<String, List<String>> {
        return try {
            WikipediaApp.instance.assets.open(ASSET_FILE_NAME).bufferedReader().useLines { lines ->
                lines.mapNotNull { parseTopicAndTitle(it) }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, titles) -> titles.distinct() }
            }
        } catch (e: Exception) {
            L.e(e)
            emptyMap()
        }
    }

    private fun parseTopicAndTitle(line: String): Pair<String, String>? {
        val separatorIndex = line.indexOf(',')
        if (separatorIndex <= 0) {
            return null
        }
        val title = parseField(line, separatorIndex + 1)
        return if (title.isEmpty()) null else line.substring(0, separatorIndex) to title
    }

    /**
     * Reads the CSV field that begins at [startIndex], honoring quoted fields, since article titles
     * may contain commas, and a doubled quote inside a quoted field stands for a literal quote.
     */
    private fun parseField(line: String, startIndex: Int): String {
        if (startIndex >= line.length) {
            return ""
        }
        if (line[startIndex] != '"') {
            val endIndex = line.indexOf(',', startIndex)
            return line.substring(startIndex, if (endIndex < 0) line.length else endIndex)
        }
        val field = StringBuilder()
        var index = startIndex + 1
        while (index < line.length) {
            val c = line[index]
            if (c == '"') {
                if (line.getOrNull(index + 1) != '"') {
                    break
                }
                index++
            }
            field.append(c)
            index++
        }
        return field.toString()
    }
}
