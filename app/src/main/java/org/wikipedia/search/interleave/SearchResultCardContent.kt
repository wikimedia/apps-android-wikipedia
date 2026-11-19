package org.wikipedia.search.interleave

import androidx.compose.ui.text.AnnotatedString
import org.wikipedia.compose.extensions.toAnnotatedString
import org.wikipedia.compose.extensions.toAnnotatedStringWithBoldQuery
import org.wikipedia.search.SearchResult
import org.wikipedia.util.StringUtil

// Remove a wiki once its reference counts become available.
private val WIKIS_WITHOUT_REFERENCE_COUNTS = setOf("ar")
private val WHITESPACE_REGEX = "\\s+".toRegex()

internal fun SearchResult.cardTitle(): AnnotatedString {
    return pageTitle.displayText.toAnnotatedStringWithBoldQuery(query = null)
}

internal fun SearchResult.cardDescription(): String? {
    return pageTitle.description?.trim()?.takeIf { it.isNotEmpty() }
}

// The section name for section matches, or null for lead matches, where the card shows "Lead".
// Only a semantic excerpt comes from the matched section. Lexical cards show the lead, so their trail is "Lead"
// even when full-text search reports the section where the words matched.
internal fun SearchResult.cardTrail(): String? {
    if (type != SearchResult.SearchResultType.SEMANTIC) {
        return null
    }
    return sectionTitle?.let { StringUtil.removeHTMLTags(it).trim() }?.takeIf { it.isNotEmpty() }
}

// Keeps superscripts, subscripts and italics, but drops the search-match highlight, which only appears in
// the article after a tap (US-3). Lexical leads should be requested as HTML extracts (prop=extracts&exintro=1,
// without explaintext=1), so they keep the same formatting as semantic passages instead of arriving flattened.
internal fun SearchResult.cardExcerpt(): AnnotatedString {
    val raw = if (type == SearchResult.SearchResultType.SEMANTIC) snippet else leadText
    val excerpt = StringUtil.fromHtml(raw.orEmpty().replace(WHITESPACE_REGEX, " ").trim()).toAnnotatedString()
    return AnnotatedString(
        text = excerpt.text,
        spanStyles = excerpt.spanStyles,
        paragraphStyles = excerpt.paragraphStyles
    )
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
