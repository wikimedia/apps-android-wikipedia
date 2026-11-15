package org.wikipedia.readinglist

import android.net.Uri
import org.junit.Assert.assertEquals
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
}
