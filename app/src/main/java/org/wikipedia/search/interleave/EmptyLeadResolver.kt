package org.wikipedia.search.interleave

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.search.SearchResult
import org.wikipedia.util.log.L

/**
 * A lexical result whose article has no lead gets its first section instead, so its card keeps the same
 * anatomy as every other result and its trail reads "Article | Section" (US-2). Disambiguation pages are left as they are.
 *
 * Extracts can't be batched without exintro, so each empty lead costs one small extracts request, made in parallel.
 * Empty leads are rare, so most lists make no extra requests.
 */
object EmptyLeadResolver {

    // Matches the lead request, enough to fill the card's excerpt lines.
    private const val EXCERPT_CHARS = 800

    suspend fun resolve(wikiSite: WikiSite, results: List<SearchResult>): List<SearchResult> {
        val emptyLeads = results.filter { !it.isDisambiguation && isEmptyLead(it.leadText) }
        if (emptyLeads.isEmpty()) {
            return results
        }
        val firstSections = coroutineScope {
            emptyLeads.map { result ->
                async { getFirstSection(wikiSite, result.pageId)?.let { result.pageId to it } }
            }.awaitAll().filterNotNull().toMap()
        }
        return results.map { result ->
            firstSections[result.pageId]?.let { section ->
                result.copy(
                    sectionTitle = section.title,
                    leadText = section.excerpt,
                    pageTitle = result.pageTitle.copy(fragment = section.anchor)
                )
            } ?: result
        }
    }

    // A failed request leaves the card as it is today, with no excerpt.
    private suspend fun getFirstSection(wikiSite: WikiSite, pageId: Int): FirstSection? {
        return try {
            ServiceFactory.get(wikiSite).getArticleStartExtract(pageId, EXCERPT_CHARS)
                .query?.pages?.firstOrNull()?.extract
                ?.let { parseFirstSection(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            L.w(e)
            null
        }
    }

    // The search returns an empty lead as markup with no text, such as <p class="mw-empty-elt"></p>.
    internal fun isEmptyLead(leadText: String?): Boolean {
        return leadText.isNullOrBlank() || Jsoup.parseBodyFragment(leadText).text().isBlank()
    }

    // The first section starts at the first heading of any level and ends at the next heading of the same or higher level.
    // The extract is flat HTML: <h2 data-mw-anchor="Ernest_Scott_Prize">Ernest Scott Prize</h2><p>...</p>
    internal fun parseFirstSection(extract: String): FirstSection? {
        val heading = Jsoup.parseBodyFragment(extract).body().selectFirst(HEADING_TAGS.joinToString()) ?: return null
        val level = heading.tagName().last().digitToInt()
        val sectionEnd = HEADING_TAGS.take(level - 1)
        val excerpt = heading.nextElementSiblings()
            .takeWhile { it.tagName() !in sectionEnd }
            .filter { it.tagName() !in HEADING_TAGS && it.hasText() }
            .joinToString("") { it.outerHtml() }
        return FirstSection(heading.text().trim(), heading.attr("data-mw-anchor").ifEmpty { null }, excerpt)
            .takeIf { it.title.isNotEmpty() && it.excerpt.isNotEmpty() }
    }

    internal data class FirstSection(val title: String, val anchor: String?, val excerpt: String)

    private val HEADING_TAGS = (2..6).map { "h$it" } // h2..h6
}
