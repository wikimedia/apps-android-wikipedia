package org.wikipedia.search.interleave

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wikipedia.Constants
import org.wikipedia.search.SearchRepository
import org.wikipedia.search.SearchResult
import org.wikipedia.search.StandardSearchRepository
import org.wikipedia.search.StandardSearchResults
import org.wikipedia.search.semantic.SemanticSearchRepository
import org.wikipedia.util.log.L

/**
 * Fetches lexical and semantic results once per query, merges them with [SearchResultsInterleaver],
 * and serves the merged list page by page, so paging never changes the list that was merged.
 */
class InterleavedSearchRepository(
    private val lexicalRepository: SearchRepository<StandardSearchResults> = StandardSearchRepository(),
    private val semanticRepository: SearchRepository<StandardSearchResults> = SemanticSearchRepository(),
    private val newSeed: () -> ULong = SearchResultsInterleaver::newSeed
) : SearchRepository<StandardSearchResults> {
    private var mergedSearchResults: MergedSearchResults? = null

    override suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean,
        countsPerLanguageCode: MutableList<Pair<String, Int>>
    ): StandardSearchResults {
        val merged = mergedSearchResults?.takeIf { it.searchTerm == searchTerm && it.languageCode == languageCode }
            ?: mergeSearchResults(searchTerm, languageCode, invokeSource, countsPerLanguageCode).also { mergedSearchResults = it }
        val slots = merged.interleaveResult.slots
        val offset = continuation ?: 0
        val pageResults = slots.drop(offset).take(batchSize).map { it.item }
        return StandardSearchResults(
            results = if (isPrefixSearch) merged.topResults + pageResults else pageResults,
            continuation = (offset + batchSize).takeIf { it < slots.size },
            xSearchIdPrefix = merged.xSearchIdPrefix,
            xSearchIdFullText = merged.xSearchIdFullText,
            xSearchIdSemantic = merged.xSearchIdSemantic
        )
    }

    private suspend fun mergeSearchResults(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        countsPerLanguageCode: MutableList<Pair<String, Int>>
    ): MergedSearchResults = coroutineScope {
        val semanticSearch = async { searchSemanticOrEmpty(searchTerm, languageCode, invokeSource) }
        val lexicalSearch = lexicalRepository.search(
            searchTerm = searchTerm,
            languageCode = languageCode,
            invokeSource = invokeSource,
            batchSize = RESULTS_PER_SEARCH,
            countsPerLanguageCode = countsPerLanguageCode
        )
        val semanticSearchResults = semanticSearch.await()

        // Tab, history and reading list results stay on top of the list and are never merged.
        val (lexicalResults, topResults) = lexicalSearch.results.partition {
            it.type == SearchResult.SearchResultType.PREFIX || it.type == SearchResult.SearchResultType.FULL_TEXT
        }
        val topResultTitles = topResults.map { it.pageTitle.prefixedText }.toSet()
        val semanticResults = semanticSearchResults.results.filterNot { it.pageTitle.prefixedText in topResultTitles }

        MergedSearchResults(
            searchTerm = searchTerm,
            languageCode = languageCode,
            topResults = topResults,
            interleaveResult = SearchResultsInterleaver.merge(lexicalResults, semanticResults, LIST_LENGTH, newSeed()) { it.pageId.toLong() },
            xSearchIdPrefix = lexicalSearch.xSearchIdPrefix,
            xSearchIdFullText = lexicalSearch.xSearchIdFullText,
            xSearchIdSemantic = semanticSearchResults.xSearchIdSemantic
        )
    }

    // A failed semantic search must not break search, so the reader gets the lexical results instead.
    private suspend fun searchSemanticOrEmpty(searchTerm: String, languageCode: String, invokeSource: Constants.InvokeSource): StandardSearchResults {
        return try {
            semanticRepository.search(searchTerm, languageCode, invokeSource, batchSize = RESULTS_PER_SEARCH)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            L.w(e)
            StandardSearchResults(emptyList(), continuation = null)
        }
    }

    private class MergedSearchResults(
        val searchTerm: String,
        val languageCode: String,
        val topResults: List<SearchResult>,
        val interleaveResult: InterleaveResult<SearchResult>,
        val xSearchIdPrefix: String?,
        val xSearchIdFullText: String?,
        val xSearchIdSemantic: String?
    )

    companion object {
        private const val LIST_LENGTH = 10
        private const val RESULTS_PER_SEARCH = 10
    }
}
