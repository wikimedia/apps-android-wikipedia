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

    // The extract is flat HTML, already free of references, links and tables:
    // <p class="mw-empty-elt"></p><h2 data-mw-anchor="Ernest_Scott_Prize">Ernest Scott Prize</h2><p>...</p>
    // The excerpt is the text between the first heading and the next h2, so a short section doesn't run into the next one.
    internal fun parseFirstSection(extract: String): FirstSection? {
        val heading = Jsoup.parseBodyFragment(extract).body().selectFirst("h2") ?: return null
        val title = heading.text().trim().takeIf { it.isNotEmpty() } ?: return null
        val excerpt = heading.nextElementSiblings()
            .takeWhile { it.tagName() != "h2" }
            .filter { !it.tagName().matches(HEADING_TAG) && it.text().isNotBlank() }
            .joinToString("") { it.outerHtml() }
            .takeIf { it.isNotEmpty() } ?: return null
        return FirstSection(title, heading.attr("data-mw-anchor").takeIf { it.isNotEmpty() }, excerpt)
    }

    internal data class FirstSection(val title: String, val anchor: String?, val excerpt: String)

    private val HEADING_TAG = "h[1-6]".toRegex()
}
