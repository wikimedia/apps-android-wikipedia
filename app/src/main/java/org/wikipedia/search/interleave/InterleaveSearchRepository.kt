package org.wikipedia.search.interleave

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.search.SearchResult

class InterleaveSearchRepository {
    suspend fun loadLeads(wikiSite: WikiSite, results: List<SearchResult>): List<SearchResult> {
        val needLead = results.filter { it.type != SearchResult.SearchResultType.SEMANTIC && it.leadText == null }
        if (needLead.isEmpty()) {
            return results
        }
        val leadsByPageId = mutableMapOf<Int, String>()
        val leadsByTitle = mutableMapOf<String, String>()
        needLead.map { it.pageTitle.prefixedText }.distinct().chunked(EXTRACTS_BATCH_SIZE).forEach { titles ->
            val pages = runCatching {
                ServiceFactory.get(wikiSite).getInfoWithExtractsByPageTitles(titles.joinToString("|"))
            }.getOrNull()?.query?.pages.orEmpty()
            pages.forEach { page ->
                val extract = page.extract ?: return@forEach
                if (page.pageId != 0) {
                    leadsByPageId[page.pageId] = extract
                }
                leadsByTitle[page.title] = extract
            }
        }
        return results.map { result ->
            if (result.type == SearchResult.SearchResultType.SEMANTIC || result.leadText != null) {
                return@map result
            }
            val lead = leadsByPageId[result.pageId].takeIf { result.pageId != 0 }
                ?: leadsByTitle[result.pageTitle.prefixedText]
            // An empty string marks "loaded, but the article has no lead", so it is not requested again.
            result.copy(leadText = lead.orEmpty())
        }
    }

    suspend fun loadAttribution(wikiSite: WikiSite, result: SearchResult): SearchResult {
        val trustAndRelevance = runCatching {
            ServiceFactory.getCoreRest(wikiSite).getAttribution(result.pageTitle.prefixedText)
        }.getOrNull()?.trustAndRelevance ?: return result
        return result.copy(
            editCounts = trustAndRelevance.contributorCounts?.totalUnique,
            referenceCounts = trustAndRelevance.referenceCount,
            lastUpdated = trustAndRelevance.lastUpdated
        )
    }

    companion object {
        private const val EXTRACTS_BATCH_SIZE = 20
    }
}
