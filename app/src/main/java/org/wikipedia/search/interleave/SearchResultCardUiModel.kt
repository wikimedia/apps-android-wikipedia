package org.wikipedia.search.interleave

import org.wikipedia.search.SearchResult
import org.wikipedia.util.StringUtil

data class SearchResultCardUiModel(
    val pageId: Int,
    val languageCode: String,
    val title: String,
    val description: String?,
    val trail: String?,
    val excerpt: String,
    val excerptType: ExcerptType,
    val thumbUrl: String?,
    val contributorCount: Int?,
    val referenceCount: Int?,
    val isAttributionLoading: Boolean
) {
    val displayFormat get() = SearchResultDisplayFormat(excerptType, hasThumbnail = thumbUrl != null)

    val isAttributionVisible get() = contributorCount != null || referenceCount != null
}

enum class ExcerptType(val logValue: String) {
    LEAD("lead"),
    PASSAGE("passage"),
    NONE("none")
}

data class SearchResultDisplayFormat(
    val excerptType: ExcerptType,
    val hasThumbnail: Boolean
)

object AttributionAvailability {
    private val WIKIS_WITHOUT_CONTRIBUTOR_COUNTS = emptySet<String>()
    private val WIKIS_WITHOUT_REFERENCE_COUNTS = setOf("ar")

    fun hasContributorCounts(languageCode: String) = languageCode !in WIKIS_WITHOUT_CONTRIBUTOR_COUNTS

    fun hasReferenceCounts(languageCode: String) = languageCode !in WIKIS_WITHOUT_REFERENCE_COUNTS
}

fun SearchResult.toCardUiModel(isAttributionLoading: Boolean = false): SearchResultCardUiModel {
    val languageCode = pageTitle.wikiSite.languageCode
    val excerpt = excerptText()
    return SearchResultCardUiModel(
        pageId = pageId,
        languageCode = languageCode,
        title = StringUtil.removeHTMLTags(pageTitle.displayText).trim(),
        description = pageTitle.description?.trim()?.takeIf { it.isNotEmpty() },
        trail = sectionTrail(),
        excerpt = excerpt,
        excerptType = when {
            excerpt.isEmpty() -> ExcerptType.NONE
            type == SearchResult.SearchResultType.SEMANTIC -> ExcerptType.PASSAGE
            else -> ExcerptType.LEAD
        },
        thumbUrl = pageTitle.thumbUrl?.takeIf { it.isNotEmpty() },
        contributorCount = editCounts.takeIf { AttributionAvailability.hasContributorCounts(languageCode) },
        referenceCount = referenceCounts.takeIf { AttributionAvailability.hasReferenceCounts(languageCode) },
        isAttributionLoading = isAttributionLoading
    )
}

private fun SearchResult.excerptText(): String {
    val raw = if (type == SearchResult.SearchResultType.SEMANTIC) snippet else leadText
    return StringUtil.removeHTMLTags(raw.orEmpty()).replace(WHITESPACE_REGEX, " ").trim()
}

private fun SearchResult.sectionTrail(): String? {
    val section = sectionTitle?.let { StringUtil.removeHTMLTags(it).trim() }.orEmpty()
    if (section.isEmpty()) {
        return null
    }
    val article = StringUtil.removeHTMLTags(pageTitle.displayText).trim()
    return "$article$TRAIL_SEPARATOR$section"
}

private const val TRAIL_SEPARATOR = " | "
private val WHITESPACE_REGEX = "\\s+".toRegex()
