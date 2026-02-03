package org.wikipedia.history

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

@RunWith(RobolectricTestRunner::class)
class HistoryEntryTest {

    @Test
    fun testPageTitlePropertiesAreStored() {
        val pageTitle = getPageTitle()
        val historyEntry = HistoryEntry(pageTitle, 0)
        assertEquals(TITLE, historyEntry.apiTitle)
        assertEquals(DISPLAY_TEXT, historyEntry.displayTitle)
        assertEquals(NAMESPACE, historyEntry.namespace)
        assertEquals(WIKI_SITE.authority(), historyEntry.authority)
        assertEquals(WIKI_SITE.languageCode, historyEntry.lang)
        assertEquals(pageTitle, historyEntry.title)
    }

    companion object {
        private val WIKI_SITE = WikiSite.forLanguageCode("en")
        private const val TITLE = "TITLE"
        private const val DISPLAY_TEXT = "DISPLAY_TEXT"
        private const val NAMESPACE = "NAMESPACE"
        private const val DESCRIPTION = "DESCRIPTION"

        fun getPageTitle(): PageTitle {
            val pageTitle = PageTitle(
                NAMESPACE, TITLE, WIKI_SITE
            )
            pageTitle.displayText = DISPLAY_TEXT
            pageTitle.description = DESCRIPTION

            return pageTitle
        }
    }
}
