package org.wikipedia.search.interleave

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        val excerpt = result(SearchResult.SearchResultType.SEMANTIC, snippet = "The <span class=\"searchmatch\">tower</span> is  tall.").cardExcerpt()
        assertEquals("The tower is tall.", excerpt.text)
        assertTrue(excerpt.spanStyles.none { it.item.background != Color.Unspecified })
    }

    @Test
    fun testExcerptHasNoLinks() {
        val excerpt = result(SearchResult.SearchResultType.SEMANTIC, snippet = "The <a href=\"./Paris\"><i>tower</i></a> is tall.").cardExcerpt()
        assertEquals("The tower is tall.", excerpt.text)
        assertTrue(excerpt.getLinkAnnotations(0, excerpt.length).isEmpty())
        assertTrue(excerpt.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun testLeadParagraphsFlowAsOnePassage() {
        val excerpt = result(SearchResult.SearchResultType.PREFIX, leadText = "<p>First paragraph.</p>\n<p>Second<br/>line.</p><ul><li>Item</li></ul>").cardExcerpt()
        assertEquals("First paragraph. Second line. Item", excerpt.text)
    }

    @Test
    fun testLeadCutByTheApiHasNoEllipsis() {
        assertEquals("The tower was", result(SearchResult.SearchResultType.PREFIX, leadText = "<p>The <i>tower</i> was...</p>").cardExcerpt().text)
        assertEquals("The tower was", result(SearchResult.SearchResultType.PREFIX, leadText = "<p>The tower was\u2026</p>").cardExcerpt().text)
        assertEquals("To be continued...", result(SearchResult.SearchResultType.SEMANTIC, snippet = "To be continued...").cardExcerpt().text)
    }

    @Test
    fun testExcerptHasNoBold() {
        val excerpt = result(SearchResult.SearchResultType.PREFIX, leadText = "<p>The <b>Eiffel Tower</b> is a <b><i>tower</i></b>.</p>").cardExcerpt()
        assertEquals("The Eiffel Tower is a tower.", excerpt.text)
        assertTrue(excerpt.spanStyles.none { it.item.fontWeight != null })
        assertTrue(excerpt.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun testExcerptKeepsSuperscripts() {
        val excerpt = result(SearchResult.SearchResultType.SEMANTIC, snippet = "About 10<sup>6</sup> km away.").cardExcerpt()
        assertEquals("About 106 km away.", excerpt.text)
        assertTrue(excerpt.spanStyles.any { it.item.baselineShift == BaselineShift.Superscript && excerpt.text.substring(it.start, it.end) == "6" })
    }

    @Test
    fun testLexicalExcerptIsLeadNotSearchSnippet() {
        val result = result(SearchResult.SearchResultType.FULL_TEXT, snippet = "<span class=\"searchmatch\">match</span>", leadText = "The lead.")
        assertEquals("The lead.", result.cardExcerpt().text)
    }

    @Test
    fun testEmptyLeadHasNoExcerpt() {
        assertEquals("", result(SearchResult.SearchResultType.PREFIX, leadText = "").cardExcerpt().text)
        assertEquals("", result(SearchResult.SearchResultType.PREFIX).cardExcerpt().text)
    }

    @Test
    fun testTitleKeepsFormatting() {
        val title = result(SearchResult.SearchResultType.PREFIX, displayText = "<i>Titanic</i> (film)").cardTitle()
        assertEquals("Titanic (film)", title.text)
        assertTrue(title.spanStyles.any { it.item.fontStyle == FontStyle.Italic && it.start == 0 && it.end == "Titanic".length })
    }

    @Test
    fun testBlankDescriptionIsHidden() {
        assertNull(result(SearchResult.SearchResultType.PREFIX, description = " ").cardDescription())
    }

    @Test
    fun testTrail() {
        assertEquals("History", result(SearchResult.SearchResultType.SEMANTIC, sectionTitle = "<b>History</b>").cardTrail())
        assertNull(result(SearchResult.SearchResultType.SEMANTIC, sectionTitle = " ").cardTrail())
        assertNull(result(SearchResult.SearchResultType.PREFIX).cardTrail())
    }

    @Test
    fun testLexicalTrailIsLeadEvenWithASectionTitle() {
        assertNull(result(SearchResult.SearchResultType.FULL_TEXT, sectionTitle = "Tour Eiffel").cardTrail())
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
