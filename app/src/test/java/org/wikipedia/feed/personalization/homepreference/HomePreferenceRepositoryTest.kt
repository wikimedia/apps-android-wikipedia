package org.wikipedia.feed.personalization.homepreference

import android.net.Uri
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.PageTitle

@RunWith(RobolectricTestRunner::class)
class HomePreferenceRepositoryTest {

    private val historyEntryWithImageDao = mockk<HistoryEntryWithImageDao>()

    private val repository = HomePreferenceRepository(
        context = mockk(relaxed = true),
        historyEntryWithImageDao = historyEntryWithImageDao,
        wikiSite = WikiSite(Uri.parse(""), "en")
    )

    @Test
    fun `when two topics are selected, returns two items from the most recent and one from the other`() {
        val content = runBlocking {
            repository.getPersonalizedPreviewContent(
                selectedArticles = emptySet(),
                contentByTopic = mapOf(
                    "architecture" to topicContent("architecture"),
                    "sports" to topicContent("sports")
                )
            )
        }

        assertEquals(
            listOf("sports 1", "sports 2", "architecture 1"),
            content.map { it.title }
        )
    }

    @Test
    fun `when one topic is selected, returns all three articles from it`() {
        val content = runBlocking {
            repository.getPersonalizedPreviewContent(
                selectedArticles = emptySet(),
                contentByTopic = mapOf("architecture" to topicContent("architecture"))
            )
        }

        assertEquals(
            listOf("architecture 1", "architecture 2", "architecture 3"),
            content.map { it.title }
        )
    }

    @Test
    fun `when more topics are selected than there are slots, the oldest ones are left out`() {
        val content = runBlocking {
            repository.getPersonalizedPreviewContent(
                selectedArticles = emptySet(),
                contentByTopic = mapOf(
                    "architecture" to topicContent("architecture"),
                    "sports" to topicContent("sports"),
                    "biology" to topicContent("biology"),
                    "history" to topicContent("history"),
                    "music" to topicContent("music")
                )
            )
        }

        assertEquals(
            listOf("music 1", "history 1", "biology 1"),
            content.map { it.title }
        )
        assertTrue(
            "the two earliest topics should contribute nothing",
            content.none { it.tag == "architecture" || it.tag == "sports" }
        )
    }

    @Test
    fun `when the most recent topic has fewer articles than its share, returns fewer than three`() {
        val content = runBlocking {
            repository.getPersonalizedPreviewContent(
                selectedArticles = emptySet(),
                contentByTopic = mapOf(
                    "architecture" to topicContent("architecture"),
                    "sports" to topicContent("sports", count = 1)
                )
            )
        }

        assertEquals(
            listOf("sports 1", "architecture 1"),
            content.map { it.title }
        )
    }

    @Test
    fun `return nothing when a selected topic has no articles even when articles are selected`() {
        val content = runBlocking {
            repository.getPersonalizedPreviewContent(
                selectedArticles = setOf(
                    PageTitle("Orange", WikiSite(Uri.parse("https://en.wikipedia.org"), "en"))
                ),
                contentByTopic = mapOf("architecture" to emptyList())
            )
        }

        assertEquals(emptyList<HomePreferenceContent>(), content)
    }

    @Test
    fun `returns nothing when fewer than three articles have been read recently`() {
        coEvery { historyEntryWithImageDao.getMostRecentEntriesWithImage(3) } returns listOf(
            HistoryEntry(apiTitle = "Orange"),
            HistoryEntry(apiTitle = "Watermelon")
        )

        val content = runBlocking {
            repository.getPersonalizedPreviewContent(
                selectedArticles = emptySet(),
                contentByTopic = emptyMap()
            )
        }

        assertEquals(emptyList<HomePreferenceContent>(), content)
    }

    private fun topicContent(topicId: String, count: Int = 6): List<HomePreferenceContent> {
        return (1..count).map { index ->
            HomePreferenceContent(
                title = "$topicId $index",
                description = null,
                imageUrl = null,
                tag = topicId
            )
        }
    }
}
