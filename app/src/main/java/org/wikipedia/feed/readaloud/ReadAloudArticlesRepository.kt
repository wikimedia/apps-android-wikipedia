package org.wikipedia.feed.readaloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.log.L
import java.time.LocalDate

/**
 * The lead-section recording of one article, and the captions that accompany it.
 */
data class ReadAloudMedia(
    val audioUrl: String,
    val captionsUrl: String,
    // When the recording was produced, and so how current the article text behind it is.
    val generatedDate: LocalDate?,
    val revisionId: Long
)

/**
 * Provides the hardcoded set of articles whose lead section has a pre-generated audio version,
 * from the bundled `ttsarticles.csv` asset.
 */
object ReadAloudArticlesRepository {
    private const val ASSET_FILE_NAME = "ttsarticles.csv"
    private const val LANGUAGE_CODE = "en"
    private const val MEDIA_BASE_URL = "https://analytics.wikimedia.org/published/datasets/ml/tts/experiment-v1/enwiki/"
    private const val AUDIO_FILE_NAME = "lead.mp3"
    private const val CAPTIONS_FILE_NAME = "lead.vtt"

    /**
     * Matches a directory whose name is all digits — a revision id — in the listing the server
     * renders, capturing the date from the "Last modified" column beside it. The `.` in a Kotlin
     * regex doesn't span newlines, so the date can only ever be read from the row the revision
     * itself was found on.
     */
    private val REVISION_ENTRY_REGEX = Regex("""href="(\d+)/".*?(\d{4}-\d{2}-\d{2})""")

    fun isSupported(wikiSite: WikiSite) = wikiSite.languageCode == LANGUAGE_CODE

    /**
     * Resolves the recording of an article's lead section. The dataset files each one under the
     * revision of the article it was generated from (`.../<pageId>/<revisionId>/lead.mp3`), and that
     * revision is discoverable only from the directory listing the server renders for the article.
     * The listing dates the recording too, which saves asking the audio itself for its
     * `Last-Modified` header.
     */
    suspend fun fetchLeadSectionMedia(summary: PageSummary): ReadAloudMedia? = withContext(Dispatchers.IO) {
        val articleUrl = "$MEDIA_BASE_URL${summary.pageId}/"
        try {
            val request = Request.Builder().url(articleUrl).build()
            OkHttpConnectionFactory.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use null
                }
                parseNewestRevision(response.body.string())?.let { (revisionId, generatedDate) ->
                    ReadAloudMedia(
                        audioUrl = "$articleUrl$revisionId/$AUDIO_FILE_NAME",
                        captionsUrl = "$articleUrl$revisionId/$CAPTIONS_FILE_NAME",
                        generatedDate = generatedDate,
                        revisionId = revisionId
                    )
                }
            }
        } catch (e: Exception) {
            L.e(e)
            null
        }
    }

    /**
     * An article normally has exactly one revision directory, but preferring the highest revision
     * keeps us on the most recently generated recording if that ever stops being true.
     */
    private fun parseNewestRevision(listingHtml: String): Pair<Long, LocalDate?>? {
        return REVISION_ENTRY_REGEX.findAll(listingHtml)
            .mapNotNull { match ->
                match.groupValues[1].toLongOrNull()?.let { it to parseListingDate(match.groupValues[2]) }
            }
            .maxByOrNull { it.first }
    }

    private fun parseListingDate(date: String): LocalDate? {
        return try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            L.e(e)
            null
        }
    }

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
