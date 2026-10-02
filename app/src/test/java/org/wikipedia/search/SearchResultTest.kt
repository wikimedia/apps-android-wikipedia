package org.wikipedia.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

@RunWith(RobolectricTestRunner::class)
class SearchResultTest {
    private val pageTitle = PageTitle("Cat", WikiSite.forLanguageCode("en"))

    @Test
    fun testFirstSearchMatchText() {
        val snippet = "The <a href='#'>domestic cat</a> is small. <span class=\"searchmatch\">Cats have excellent night vision</span> " +
                "and <span class=\"searchmatch\">hearing</span>."
        assertEquals("Cats have excellent night vision", SearchResult(pageTitle, snippet = snippet).firstSearchMatchText)
    }

    @Test
    fun testFirstSearchMatchTextDecodesHtml() {
        val snippet = "<span class='searchmatch'>Tom &amp; <b>Jerry</b></span>"
        assertEquals("Tom & Jerry", SearchResult(pageTitle, snippet = snippet).firstSearchMatchText)
    }

    @Test
    fun testFirstSearchMatchTextWithoutMatch() {
        assertNull(SearchResult(pageTitle, snippet = "No highlighted text here").firstSearchMatchText)
        assertNull(SearchResult(pageTitle, snippet = "<span class=\"searchmatch\"> </span>").firstSearchMatchText)
        assertNull(SearchResult(pageTitle).firstSearchMatchText)
    }
}
