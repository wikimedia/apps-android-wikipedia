package org.wikipedia.readinglist

import android.text.Spanned
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.StringUtil

class ReadingListBehaviorsUtilTest {

    private val wikiSite = mockk<WikiSite>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(StringUtil)
        every { StringUtil.fromHtml(any()) } answers {
            val text = firstArg<String?>().orEmpty()
            val spanned = mockk<Spanned>()
            every { spanned.toString() } returns text
            spanned
        }
    }

    @After
    fun tearDown() {
        unmockkObject(StringUtil)
    }

    @Test
    fun testApplySearchQueryReturnsAllListsWhenQueryEmptyOrNull() {
        val list1 = ReadingList("List 1", "Description 1")
        val list2 = ReadingList("List 2", "Description 2")
        val allLists = listOf(list1, list2)

        val nullResult = ReadingListBehaviorsUtil.applySearchQuery(null, allLists)
        assertEquals(2, nullResult.size)

        val emptyResult = ReadingListBehaviorsUtil.applySearchQuery("", allLists)
        assertEquals(2, emptyResult.size)
    }

    @Test
    fun testApplySearchQueryMatchesArticleTitle() {
        val list = ReadingList("My Collection", "A custom list")
        val page = ReadingListPage(
            wiki = wikiSite,
            namespace = Namespace.MAIN,
            displayTitle = "Synthesia (videogame)",
            apiTitle = "Synthesia_(videogame)",
            description = "Computer software to help piano practice",
            offline = false
        )
        list.pages.add(page)

        val result = ReadingListBehaviorsUtil.applySearchQuery("Synthesia", listOf(list))
        assertTrue(result.contains(page))
    }

    @Test
    fun testApplySearchQueryMatchesArticleDescription() {
        val list = ReadingList("My Collection", "A custom list")
        val page = ReadingListPage(
            wiki = wikiSite,
            namespace = Namespace.MAIN,
            displayTitle = "Synthesia (videogame)",
            apiTitle = "Synthesia_(videogame)",
            description = "Computer software to help piano practice",
            offline = false
        )
        list.pages.add(page)

        val result = ReadingListBehaviorsUtil.applySearchQuery("software", listOf(list))
        assertTrue(result.contains(page))

        val resultPiano = ReadingListBehaviorsUtil.applySearchQuery("piano", listOf(list))
        assertTrue(resultPiano.contains(page))
    }

    @Test
    fun testApplySearchQueryMatchesAccentInvariant() {
        val list = ReadingList("Cafés", "Places to drink café")
        val page = ReadingListPage(
            wiki = wikiSite,
            namespace = Namespace.MAIN,
            displayTitle = "Café de Flore",
            apiTitle = "Café_de_Flore",
            description = "Famous French café in Paris",
            offline = false
        )
        list.pages.add(page)

        val result1 = ReadingListBehaviorsUtil.applySearchQuery("cafe", listOf(list))
        assertTrue(result1.contains(page))
        assertTrue(result1.contains(list))

        val result2 = ReadingListBehaviorsUtil.applySearchQuery("café", listOf(list))
        assertTrue(result2.contains(page))
        assertTrue(result2.contains(list))
    }

    @Test
    fun testApplySearchQueryMatchesCollectionDescription() {
        val list = ReadingList("Favorites", "Interesting astronomy articles")
        val result = ReadingListBehaviorsUtil.applySearchQuery("astronomy", listOf(list))
        assertTrue(result.contains(list))
    }
}
