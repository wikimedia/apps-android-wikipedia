package org.wikipedia.history

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
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
        MatcherAssert.assertThat(historyEntry.description, Matchers.`is`(DESCRIPTION))
        MatcherAssert.assertThat(historyEntry.apiTitle, Matchers.`is`(TITLE))
        MatcherAssert.assertThat(historyEntry.displayTitle, Matchers.`is`(DISPLAY_TEXT))
        MatcherAssert.assertThat(historyEntry.namespace, Matchers.`is`(NAMESPACE))
        MatcherAssert.assertThat(historyEntry.authority, Matchers.`is`(WIKI_SITE.authority()))
        MatcherAssert.assertThat(historyEntry.lang, Matchers.`is`(WIKI_SITE.languageCode))
        MatcherAssert.assertThat(historyEntry.title, Matchers.`is`(pageTitle))
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
