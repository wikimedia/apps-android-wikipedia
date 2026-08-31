package org.wikipedia.feed.personalization

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.personalization.db.dao.InterestArticleDao
import org.wikipedia.feed.personalization.db.dao.InterestTopicDao
import org.wikipedia.feed.personalization.interest.InterestSelectionRepository
import org.wikipedia.feed.random.RandomClient
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.db.ReadingListPageDao
import org.wikipedia.staticdata.MainPageNameData

@RunWith(RobolectricTestRunner::class)
class InterestSelectionRepositoryTest {
    private val wikiSite = WikiSite(Uri.parse(""), "en")
    private val interestTopicDao = mockk<InterestTopicDao>()
    private val interestArticleDao = mockk<InterestArticleDao>()
    private val historyEntryWithImageDao = mockk<HistoryEntryWithImageDao>()
    private val readingListPageDao = mockk<ReadingListPageDao>()

    private val repository = InterestSelectionRepository(
        interestTopicDao = interestTopicDao,
        interestArticleDao = interestArticleDao,
        historyEntryWithImageDao = historyEntryWithImageDao,
        readingListPageDao = readingListPageDao,
        wikiSite = wikiSite
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `interleaves recently read articles with recently saved ones`() {
        coEvery { historyEntryWithImageDao.findEntryForReadMore(20, 0, "en") } returns listOf(
            historyEntry("History 1"),
            historyEntry("History 2"),
            historyEntry("History 3")
        )
        coEvery { readingListPageDao.getMostRecentSavedPagesByLang("en", 20) } returns listOf(
            savedPage("Saved 1"),
            savedPage("Saved 2"),
            savedPage("Saved 3")
        )

        val articles = runBlocking { repository.loadInitialArticles() }

        assertEquals(
            listOf("History 1", "Saved 1", "History 2", "Saved 2", "History 3", "Saved 3"),
            articles.map { it.displayText }
        )
    }

    @Test
    fun `an article that is both recently read and saved appears once`() {
        coEvery { historyEntryWithImageDao.findEntryForReadMore(20, 0, "en") } returns listOf(
            historyEntry("Kangaroo"),
            historyEntry("Koala"),
            historyEntry("Wombat"),
            historyEntry("Emu")
        )
        coEvery { readingListPageDao.getMostRecentSavedPagesByLang("en", 20) } returns listOf(
            savedPage("Kangaroo"),
            savedPage("Platypus"),
            savedPage("Echidna"),
            savedPage("Dingo")
        )

        val articles = runBlocking { repository.loadInitialArticles() }

        assertEquals(1, articles.count { it.displayText == "Kangaroo" })
        assertEquals(
            listOf("Kangaroo", "Koala", "Platypus", "Wombat", "Echidna", "Emu", "Dingo"),
            articles.map { it.displayText }
        )
    }

    @Test
    fun `drops the main page and anything outside the article namespace`() {
        coEvery { historyEntryWithImageDao.findEntryForReadMore(20, 0, "en") } returns listOf(
            historyEntry(MainPageNameData.valueFor("en")),
            historyEntry("Koala"),
            historyEntry("Wombat"),
            historyEntry("Emu")
        )
        coEvery { readingListPageDao.getMostRecentSavedPagesByLang("en", 20) } returns listOf(
            savedPage("Talk:Kangaroo"),
            savedPage("Platypus"),
            savedPage("Echidna"),
            savedPage("Dingo")
        )

        val articles = runBlocking { repository.loadInitialArticles() }

        assertEquals(
            listOf("Koala", "Platypus", "Wombat", "Echidna", "Emu", "Dingo"),
            articles.map { it.displayText }
        )
    }

    @Test
    fun `pads with random articles when there are fewer than six to show`() {
        coEvery { historyEntryWithImageDao.findEntryForReadMore(20, 0, "en") } returns listOf(
            historyEntry("Koala"),
            historyEntry("Wombat")
        )
        coEvery { readingListPageDao.getMostRecentSavedPagesByLang("en", 20) } returns listOf(
            savedPage("Platypus"),
            savedPage("Echidna")
        )
        mockkObject(RandomClient)
        coEvery { RandomClient.getRandomPages(wikiSite, 6) } returns listOf(
            pageTitle("Random 1"),
            pageTitle("Random 2"),
            pageTitle("Random 3")
        )

        val articles = runBlocking { repository.loadInitialArticles() }

        assertEquals(
            listOf("Koala", "Platypus", "Wombat", "Echidna", "Random 1", "Random 2", "Random 3"),
            articles.map { it.displayText }
        )
    }

    @Test
    fun `does not reach for random articles once there are six to show`() {
        coEvery { historyEntryWithImageDao.findEntryForReadMore(20, 0, "en") } returns listOf(
            historyEntry("Koala"),
            historyEntry("Wombat"),
            historyEntry("Emu")
        )
        coEvery { readingListPageDao.getMostRecentSavedPagesByLang("en", 20) } returns listOf(
            savedPage("Platypus"),
            savedPage("Echidna"),
            savedPage("Dingo")
        )
        mockkObject(RandomClient)

        val articles = runBlocking { repository.loadInitialArticles() }

        coVerify(exactly = 0) { RandomClient.getRandomPages(any(), any()) }
        assertEquals(6, articles.size)
    }

    @Test
    fun `a duplicate article does not count towards the six`() {
        val readAndSaved = listOf("Kangaroo", "Koala", "Wombat", "Emu", "Platypus")
        coEvery { historyEntryWithImageDao.findEntryForReadMore(20, 0, "en") } returns
            readAndSaved.map { historyEntry(it) }
        coEvery { readingListPageDao.getMostRecentSavedPagesByLang("en", 20) } returns
            readAndSaved.map { savedPage(it) }
        mockkObject(RandomClient)
        coEvery { RandomClient.getRandomPages(wikiSite, 6) } returns listOf(pageTitle("Random 1"))

        val articles = runBlocking { repository.loadInitialArticles() }

        coVerify { RandomClient.getRandomPages(wikiSite, 6) }
        assertEquals(
            listOf("Kangaroo", "Koala", "Wombat", "Emu", "Platypus", "Random 1"),
            articles.map { it.displayText }
        )
    }

    private fun historyEntry(title: String): HistoryEntry {
        return HistoryEntry(title = pageTitle(title), source = HistoryEntry.SOURCE_SEARCH)
    }

    private fun savedPage(title: String): ReadingListPage {
        return ReadingListPage(pageTitle(title))
    }

    private fun pageTitle(title: String): PageTitle {
        return PageTitle(
            text = title,
            wiki = wikiSite,
            thumbUrl = "https://example.org/${title.replace(' ', '_')}.jpg",
            description = "description of $title",
            displayText = title
        )
    }
}
