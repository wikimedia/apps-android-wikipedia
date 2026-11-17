package org.wikipedia.search.interleave

import org.wikipedia.search.SearchResult
import org.wikipedia.util.StringUtil

// Remove a wiki once its reference counts become available.
private val WIKIS_WITHOUT_REFERENCE_COUNTS = setOf("ar")
private val WHITESPACE_REGEX = "\\s+".toRegex()

internal fun SearchResult.cardTitle(): String {
    return StringUtil.removeHTMLTags(pageTitle.displayText).trim()
}

internal fun SearchResult.cardDescription(): String? {
    return pageTitle.description?.trim()?.takeIf { it.isNotEmpty() }
}

// The section name for section matches, or null for lead matches, where the card shows "Lead".
internal fun SearchResult.cardTrail(): String? {
    return sectionTitle?.let { StringUtil.removeHTMLTags(it).trim() }?.takeIf { it.isNotEmpty() }
}

internal fun SearchResult.cardExcerpt(): String {
    val raw = if (type == SearchResult.SearchResultType.SEMANTIC) snippet else leadText
    return StringUtil.removeHTMLTags(raw.orEmpty()).replace(WHITESPACE_REGEX, " ").trim()
}

internal fun SearchResult.cardThumbUrl(): String? {
    return pageTitle.thumbUrl?.takeIf { it.isNotEmpty() }
}

internal fun SearchResult.cardContributorCount(): Int? {
    return editCounts
}

internal fun SearchResult.cardReferenceCount(): Int? {
    return referenceCounts.takeIf { pageTitle.wikiSite.languageCode !in WIKIS_WITHOUT_REFERENCE_COUNTS }
}
