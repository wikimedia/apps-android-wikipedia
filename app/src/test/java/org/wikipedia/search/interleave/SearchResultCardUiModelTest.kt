package org.wikipedia.search.interleave

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult

@RunWith(RobolectricTestRunner::class)
class SearchResultCardUiModelTest {

    @Test
    fun testSemanticExcerptIsPassageWithoutHighlight() {
        val model = map(result(SearchResult.SearchResultType.SEMANTIC, snippet = "The <span class=\"searchmatch\">tower</span> is  tall."))
        assertEquals("The tower is tall.", model.excerpt)
        assertEquals(ExcerptType.PASSAGE, model.excerptType)
    }

    @Test
    fun testLexicalExcerptIsLeadNotSearchSnippet() {
        val model = map(result(SearchResult.SearchResultType.FULL_TEXT, snippet = "<span class=\"searchmatch\">match</span>", leadText = "The lead."))
        assertEquals("The lead.", model.excerpt)
        assertEquals(ExcerptType.LEAD, model.excerptType)
    }

    @Test
    fun testEmptyLeadHasNoExcerpt() {
        assertEquals(ExcerptType.NONE, map(result(SearchResult.SearchResultType.PREFIX, leadText = "")).excerptType)
        assertEquals(ExcerptType.NONE, map(result(SearchResult.SearchResultType.PREFIX)).excerptType)
    }

    @Test
    fun testTitleAndDescription() {
        val model = map(result(SearchResult.SearchResultType.PREFIX, displayText = "<i>Titanic</i>", description = " "))
        assertEquals("Titanic", model.title)
        assertNull(model.description)
    }

    @Test
    fun testTrail() {
        assertEquals("Test | History", map(result(SearchResult.SearchResultType.SEMANTIC, sectionTitle = "History")).trail)
        assertNull(map(result(SearchResult.SearchResultType.SEMANTIC, sectionTitle = " ")).trail)
        assertNull(map(result(SearchResult.SearchResultType.PREFIX)).trail)
    }

    @Test
    fun testDisplayFormat() {
        val format = map(result(SearchResult.SearchResultType.SEMANTIC, snippet = "x", thumbUrl = "https://example")).displayFormat
        assertEquals(SearchResultDisplayFormat(ExcerptType.PASSAGE, hasThumbnail = true), format)
        assertFalse(map(result(SearchResult.SearchResultType.PREFIX, thumbUrl = "")).displayFormat.hasThumbnail)
    }

    @Test
    fun testMissingCountOnOneResultOnlyHidesItOnThatCard() {
        val withCounts = map(result(SearchResult.SearchResultType.SEMANTIC, editCounts = 10, referenceCounts = 5))
        val missingReferences = map(result(SearchResult.SearchResultType.PREFIX, editCounts = 3, referenceCounts = null))
        assertEquals(5, withCounts.referenceCount)
        assertNull(missingReferences.referenceCount)
        assertEquals(3, missingReferences.contributorCount)
        assertTrue(missingReferences.isAttributionVisible)
    }

    @Test
    fun testWikiWithoutReferenceCountsHidesThemOnEveryResult() {
        val model = map(result(SearchResult.SearchResultType.SEMANTIC, languageCode = "ar", editCounts = 10, referenceCounts = 5))
        assertEquals(10, model.contributorCount)
        assertNull(model.referenceCount)
    }

    @Test
    fun testZeroReferencesIsShown() {
        assertEquals(0, map(result(SearchResult.SearchResultType.PREFIX, editCounts = 1, referenceCounts = 0)).referenceCount)
    }

    @Test
    fun testNoAttributionHidesRow() {
        assertFalse(map(result(SearchResult.SearchResultType.PREFIX)).isAttributionVisible)
    }

    @Test
    fun testAttributionLoading() {
        assertTrue(result(SearchResult.SearchResultType.PREFIX).toCardUiModel(isAttributionLoading = true).isAttributionLoading)
        assertFalse(map(result(SearchResult.SearchResultType.PREFIX)).isAttributionLoading)
    }

    private fun map(result: SearchResult) = result.toCardUiModel()

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
