package org.wikipedia.search.interleave

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult

@RunWith(RobolectricTestRunner::class)
class SearchResultCardContentTest {

    @Test
    fun testSemanticExcerptIsPassageWithoutHighlight() {
        val result = result(SearchResult.SearchResultType.SEMANTIC, snippet = "The <span class=\"searchmatch\">tower</span> is  tall.")
        assertEquals("The tower is tall.", result.cardExcerpt())
    }

    @Test
    fun testLexicalExcerptIsLeadNotSearchSnippet() {
        val result = result(SearchResult.SearchResultType.FULL_TEXT, snippet = "<span class=\"searchmatch\">match</span>", leadText = "The lead.")
        assertEquals("The lead.", result.cardExcerpt())
    }

    @Test
    fun testEmptyLeadHasNoExcerpt() {
        assertEquals("", result(SearchResult.SearchResultType.PREFIX, leadText = "").cardExcerpt())
        assertEquals("", result(SearchResult.SearchResultType.PREFIX).cardExcerpt())
    }

    @Test
    fun testTitleAndDescription() {
        val result = result(SearchResult.SearchResultType.PREFIX, displayText = "<i>Titanic</i>", description = " ")
        assertEquals("Titanic", result.cardTitle())
        assertNull(result.cardDescription())
    }

    @Test
    fun testTrail() {
        assertEquals("History", result(SearchResult.SearchResultType.SEMANTIC, sectionTitle = "<b>History</b>").cardTrail())
        assertNull(result(SearchResult.SearchResultType.SEMANTIC, sectionTitle = " ").cardTrail())
        assertNull(result(SearchResult.SearchResultType.PREFIX).cardTrail())
    }

    @Test
    fun testThumbnail() {
        assertEquals("https://example", result(SearchResult.SearchResultType.SEMANTIC, thumbUrl = "https://example").cardThumbUrl())
        assertNull(result(SearchResult.SearchResultType.PREFIX, thumbUrl = "").cardThumbUrl())
    }

    @Test
    fun testMissingCountOnlyHidesThatFigure() {
        val result = result(SearchResult.SearchResultType.PREFIX, editCounts = 3, referenceCounts = null)
        assertEquals(3, result.cardContributorCount())
        assertNull(result.cardReferenceCount())
    }

    @Test
    fun testWikiWithoutReferenceCountsHidesThem() {
        val result = result(SearchResult.SearchResultType.SEMANTIC, languageCode = "ar", editCounts = 10, referenceCounts = 5)
        assertEquals(10, result.cardContributorCount())
        assertNull(result.cardReferenceCount())
    }

    @Test
    fun testZeroReferencesIsShown() {
        assertEquals(0, result(SearchResult.SearchResultType.PREFIX, referenceCounts = 0).cardReferenceCount())
    }

    private fun result(
        type: SearchResult.SearchResultType,
        languageCode: String = "fr",
        displayText: String? = null,
        description: String? = null,
        snippet: String? = null,
        leadText: String? = null,
        sectionTitle: String? = null,
        thumbUrl: String? = null,
        editCounts: Int? = null,
        referenceCounts: Int? = null
    ): SearchResult {
        val pageTitle = PageTitle("Test", WikiSite.forLanguageCode(languageCode)).apply {
            displayText?.let { this.displayText = it }
            this.description = description
            this.thumbUrl = thumbUrl
        }
        return SearchResult(
            pageTitle = pageTitle,
            redirectFrom = null,
            type = type,
            snippet = snippet,
            sectionTitle = sectionTitle,
            editCounts = editCounts,
            referenceCounts = referenceCounts,
            leadText = leadText
        )
    }
}
