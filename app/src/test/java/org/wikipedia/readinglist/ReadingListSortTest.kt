package org.wikipedia.readinglist

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage

@RunWith(RobolectricTestRunner::class)
class ReadingListSortTest {

    private val wikiSite = WikiSite(Uri.parse(""), "en")

    private fun page(displayTitle: String, mtime: Long) = ReadingListPage(
        wiki = wikiSite,
        namespace = Namespace.MAIN,
        displayTitle = displayTitle,
        apiTitle = displayTitle,
        mtime = mtime,
        offline = false
    )

    private fun readingList(title: String, mtime: Long = 0L) = ReadingList(title, null, mtime = mtime)

    @Test
    fun testNewestSortPutsMostRecentlyAddedPageFirst() {
        val list = ReadingList("Test list", null).apply {
            pages.add(page("Oldest", 1_000L))
            pages.add(page("Newest", 3_000L))
            pages.add(page("Middle", 2_000L))
        }

        ReadingList.sort(list, ReadingList.SORT_BY_RECENT_ASC)

        assertEquals(listOf("Newest", "Middle", "Oldest"), list.pages.map { it.apiTitle })
    }

    @Test
    fun testOldestSortPutsLeastRecentlyAddedPageFirst() {
        val list = ReadingList("Test list", null).apply {
            pages.add(page("Newest", 3_000L))
            pages.add(page("Oldest", 1_000L))
            pages.add(page("Middle", 2_000L))
        }

        ReadingList.sort(list, ReadingList.SORT_BY_RECENT_DESC)

        assertEquals(listOf("Oldest", "Middle", "Newest"), list.pages.map { it.apiTitle })
    }

    @Test
    fun testSortListsByNameAscending() {
        val lists = mutableListOf(readingList("Zeta"), readingList("Alpha"), readingList("Mike"))

        ReadingList.sort(lists, ReadingList.SORT_BY_NAME_ASC)

        assertEquals(listOf("Alpha", "Mike", "Zeta"), lists.map { it.title })
    }

    @Test
    fun testSortListsByNameDescending() {
        val lists = mutableListOf(readingList("Zeta"), readingList("Alpha"), readingList("Mike"))

        ReadingList.sort(lists, ReadingList.SORT_BY_NAME_DESC)

        assertEquals(listOf("Zeta", "Mike", "Alpha"), lists.map { it.title })
    }

    @Test
    fun testSortListsByNewestFirst() {
        val lists = mutableListOf(readingList("Oldest", 1_000L), readingList("Newest", 3_000L), readingList("Middle", 2_000L))

        ReadingList.sort(lists, ReadingList.SORT_BY_RECENT_ASC)

        assertEquals(listOf("Newest", "Middle", "Oldest"), lists.map { it.title })
    }

    @Test
    fun testSortListsByOldestFirst() {
        val lists = mutableListOf(readingList("Newest", 3_000L), readingList("Oldest", 1_000L), readingList("Middle", 2_000L))

        ReadingList.sort(lists, ReadingList.SORT_BY_RECENT_DESC)

        assertEquals(listOf("Oldest", "Middle", "Newest"), lists.map { it.title })
    }

    @Test
    fun testDefaultListStaysOnTopWhenSorting() {
        val defaultList = ReadingList("", null)
        val lists = mutableListOf(readingList("Zeta"), readingList("Alpha"), defaultList)

        ReadingList.sort(lists, ReadingList.SORT_BY_NAME_ASC)

        assertSame(defaultList, lists.first())
        assertEquals(listOf(defaultList.title, "Alpha", "Zeta"), lists.map { it.title })
    }
}
