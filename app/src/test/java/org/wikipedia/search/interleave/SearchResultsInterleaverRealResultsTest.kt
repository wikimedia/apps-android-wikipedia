package org.wikipedia.search.interleave

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.Constants
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.json.JsonUtil
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResultsViewModel
import org.wikipedia.test.TestFileUtil

@RunWith(RobolectricTestRunner::class)
class SearchResultsInterleaverRealResultsTest {
    private val wikiSite = WikiSite.forLanguageCode("fr")

    @Test
    fun testEachPageIsPlacedOnceAcrossBothLists() {
        listOf(TOUR_EIFFEL_QUERY, NAPOLEON_BONAPARTE_QUERY).forEach { query ->
            val lexical = lexicalResults(query)
            val semantic = semanticResults(query)
            val lexicalPageIds = lexical.map { it.pageId }.toSet()
            val semanticPageIds = semantic.map { it.pageId }.toSet()

            for (seed in 0uL until SEEDS) {
                val slots = merge(lexical, semantic, seed).slots
                val placedPageIds = slots.map { it.item.pageId }
                assertEquals("$query/seed_$seed", placedPageIds.distinct(), placedPageIds)
                assertEquals("$query/seed_$seed", lexicalPageIds + semanticPageIds, placedPageIds.toSet())
                slots.forEach { slot ->
                    val source = if (slot.searchType == SearchType.LEXICAL) lexical else semantic
                    assertSame("$query/seed_$seed", source[slot.rank - 1], slot.item)
                    assertEquals("$query/seed_$seed", slot.item.pageId in lexicalPageIds && slot.item.pageId in semanticPageIds, slot.alsoInOther)
                }
            }
        }
    }

    @Test
    fun testRetrieverThatPicksFirstPlacesTheArticleAtTheTopOfBothLists() {
        val lexical = lexicalResults(TOUR_EIFFEL_QUERY)
        val semantic = semanticResults(TOUR_EIFFEL_QUERY)

        val lexicalFirst = merge(lexical, semantic, LEXICAL_FIRST_SEED)
        assertEquals(SearchType.LEXICAL, lexicalFirst.coinFlips.first())
        assertSlot(lexicalFirst.slots[0], TOUR_EIFFEL, SearchType.LEXICAL, rank = 1, alsoInOther = true)
        assertSlot(lexicalFirst.slots[1], SOCIETE_D_EXPLOITATION_DE_LA_TOUR_EIFFEL, SearchType.SEMANTIC, rank = 2, alsoInOther = true)

        val semanticFirst = merge(lexical, semantic, SEMANTIC_FIRST_SEED)
        assertEquals(SearchType.SEMANTIC, semanticFirst.coinFlips.first())
        assertSlot(semanticFirst.slots[0], TOUR_EIFFEL, SearchType.SEMANTIC, rank = 1, alsoInOther = true)
        assertSlot(semanticFirst.slots[1], LA_TOUR_EIFFEL_EN_OTAGE, SearchType.LEXICAL, rank = 2, alsoInOther = false)
    }

    @Test
    fun testArticleInBothListsKeepsTheSectionOfTheRetrieverThatPlacedIt() {
        val lexical = lexicalResults(TOUR_EIFFEL_QUERY)
        val semantic = semanticResults(TOUR_EIFFEL_QUERY)
        val lexicalResult = lexical.single { it.pageId == HISTOIRE_DE_LA_TOUR_EIFFEL }
        val semanticResult = semantic.single { it.pageId == HISTOIRE_DE_LA_TOUR_EIFFEL }
        assertEquals(SearchResult.SearchResultType.FULL_TEXT, lexicalResult.type)
        assertNotEquals(lexicalResult.pageTitle.fragment, semanticResult.pageTitle.fragment)

        for (seed in 0uL until SEEDS) {
            val slot = merge(lexical, semantic, seed).slots.single { it.item.pageId == HISTOIRE_DE_LA_TOUR_EIFFEL }
            assertEquals("seed_$seed", SearchType.SEMANTIC, slot.searchType)
            assertSame("seed_$seed", semanticResult, slot.item)
        }
    }

    @Test
    fun testPrefixResultReachedThroughRedirectIsNotPlacedTwice() {
        val lexical = lexicalResults(NAPOLEON_BONAPARTE_QUERY)
        val semantic = semanticResults(NAPOLEON_BONAPARTE_QUERY)
        val redirectedResult = lexical.single { it.pageId == NAPOLEON_IER }
        val redirectedRank = lexical.indexOf(redirectedResult) + 1
        assertEquals("Napoléon Bonaparte", redirectedResult.redirectFrom)
        assertEquals(1, semantic.indexOfFirst { it.pageId == NAPOLEON_IER } + 1)

        for (seed in 0uL until SEEDS) {
            val slots = merge(lexical, semantic, seed).slots
            val slot = slots.single { it.item.pageId == NAPOLEON_IER }
            assertEquals("seed_$seed", SearchType.SEMANTIC, slot.searchType)
            assertTrue("seed_$seed", slot.alsoInOther)
            assertTrue("seed_$seed", slots.none { it.searchType == SearchType.LEXICAL && it.rank == redirectedRank })
        }
    }

    @Test
    fun testSemanticResultsFillTheListWhenLexicalIsEmpty() {
        val lexical = lexicalResults(EMPTY_PREFIX_RESULTS, EMPTY_FULL_TEXT_RESULTS)
        val semantic = semanticResults(TOUR_EIFFEL_QUERY)
        assertTrue(lexical.isEmpty())

        val result = merge(lexical, semantic, LEXICAL_FIRST_SEED)
        assertEquals(semantic, result.slots.map { it.item })
        assertTrue(result.slots.all { it.searchType == SearchType.SEMANTIC })
        assertTrue(result.coinFlips.isEmpty())
    }

    @Test
    fun testLexicalResultsFillTheListWhenSemanticIsEmpty() {
        val lexical = lexicalResults(TOUR_EIFFEL_QUERY)
        val semantic = searchResults(EMPTY_FULL_TEXT_RESULTS, SearchResult.SearchResultType.SEMANTIC)
        assertTrue(semantic.isEmpty())

        val result = merge(lexical, semantic, SEMANTIC_FIRST_SEED)
        assertEquals(lexical, result.slots.map { it.item })
        assertTrue(result.slots.all { it.searchType == SearchType.LEXICAL })
        assertTrue(result.coinFlips.isEmpty())
    }

    private fun merge(lexical: List<SearchResult>, semantic: List<SearchResult>, seed: ULong): InterleaveResult<SearchResult> {
        return SearchResultsInterleaver.merge(lexical, semantic, LIST_LENGTH, seed) { it.pageId.toLong() }
    }

    private fun assertSlot(slot: InterleaveSlot<SearchResult>, pageId: Int, searchType: SearchType, rank: Int, alsoInOther: Boolean) {
        assertEquals(pageId, slot.item.pageId)
        assertEquals(searchType, slot.searchType)
        assertEquals(rank, slot.rank)
        assertEquals(alsoInOther, slot.alsoInOther)
    }

    private fun lexicalResults(query: String): List<SearchResult> {
        return lexicalResults("${query}_prefix_search_results.json", "${query}_full_text_search_results.json")
    }

    // Mirrors the first page of StandardSearchRepository: full-text results only fill a page that prefix search leaves short.
    private fun lexicalResults(prefixFile: String, fullTextFile: String): List<SearchResult> {
        val prefixResults = searchResults(prefixFile, SearchResult.SearchResultType.PREFIX)
        val fullTextResults = if (prefixResults.size < BATCH_SIZE) searchResults(fullTextFile, SearchResult.SearchResultType.FULL_TEXT) else emptyList()
        return (prefixResults + fullTextResults).distinctBy { it.pageTitle.prefixedText }
    }

    private fun semanticResults(query: String): List<SearchResult> {
        return searchResults("${query}_semantic_search_results.json", SearchResult.SearchResultType.SEMANTIC)
    }

    private fun searchResults(file: String, type: SearchResult.SearchResultType): List<SearchResult> {
        val response = JsonUtil.decodeFromString<MwQueryResponse>(TestFileUtil.readRawFile(file))
        return SearchResultsViewModel.buildList(response, Constants.InvokeSource.SEARCH, wikiSite, type)
    }

    companion object {
        private const val LIST_LENGTH = 50
        private const val BATCH_SIZE = 10
        private const val SEEDS = 1000uL
        private const val LEXICAL_FIRST_SEED = 1234567uL
        private const val SEMANTIC_FIRST_SEED = 0uL

        private const val TOUR_EIFFEL_QUERY = "fr_tour_eiffel"
        private const val NAPOLEON_BONAPARTE_QUERY = "fr_napoleon_bonaparte"
        private const val EMPTY_PREFIX_RESULTS = "prefix_search_results_empty.json"
        private const val EMPTY_FULL_TEXT_RESULTS = "full_text_search_results_empty.json"

        private const val TOUR_EIFFEL = 1359783
        private const val LA_TOUR_EIFFEL_EN_OTAGE = 10318723
        private const val SOCIETE_D_EXPLOITATION_DE_LA_TOUR_EIFFEL = 7186932
        private const val HISTOIRE_DE_LA_TOUR_EIFFEL = 688954
        private const val NAPOLEON_IER = 676806
    }
}
