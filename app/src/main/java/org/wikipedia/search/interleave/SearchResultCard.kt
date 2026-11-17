package org.wikipedia.search.interleave

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getResources
import org.wikipedia.extensions.getString
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

private const val HEADER_MAX_LINES = 2
private const val EXCERPT_MAX_LINES = 8

@Composable
fun SearchResultCard(
    searchResult: SearchResult,
    modifier: Modifier = Modifier,
    isAttributionLoading: Boolean = false,
    relevance: Boolean? = null,
    onRelevanceClick: ((Boolean) -> Unit)? = null,
    onItemClick: () -> Unit
) {
    val context = LocalContext.current
    val languageCode = searchResult.pageTitle.wikiSite.languageCode
    val excerpt = remember(searchResult) { searchResult.cardExcerpt() }
    val contributorCount = searchResult.cardContributorCount()
    val referenceCount = searchResult.cardReferenceCount()
    val isAttributionVisible = contributorCount != null || referenceCount != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WikipediaTheme.colors.backgroundColor)
            .clickable(onClick = onItemClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (onRelevanceClick != null) 4.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = remember(searchResult) { searchResult.cardTitle() },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = WikipediaTheme.colors.primaryColor,
                    maxLines = HEADER_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
                searchResult.cardDescription()?.let {
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.secondaryColor,
                        maxLines = HEADER_MAX_LINES,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(14.dp),
                        painter = painterResource(R.drawable.ic_subdirectory_arrow_right_black_24dp),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = remember(searchResult) { searchResult.cardTrail() }
                            ?: context.getString(languageCode, R.string.search_result_trail_lead),
                        style = MaterialTheme.typography.bodySmall,
                        color = WikipediaTheme.colors.primaryColor,
                        maxLines = HEADER_MAX_LINES,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            searchResult.cardThumbUrl()?.let { url ->
                AsyncImage(
                    model = ImageService.getRequest(context, url = url),
                    placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                    error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }

        if (excerpt.text.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = WikipediaTheme.colors.borderColor,
                thickness = 1.dp
            )
            Text(
                text = excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.primaryColor,
                maxLines = EXCERPT_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = context.getString(languageCode, R.string.semantic_search_results_read_in_article_label),
                style = MaterialTheme.typography.labelMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
        }

        if (isAttributionLoading || isAttributionVisible) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                color = WikipediaTheme.colors.borderColor,
                thickness = 1.dp
            )
        }

        if (isAttributionLoading) {
            // Keep the row's space while counts load, so the card doesn't grow when they arrive.
            SearchResultAttributionRow(
                modifier = Modifier
                    .alpha(0f)
                    .clearAndSetSemantics {},
                languageCode = languageCode,
                contributorCount = 0,
                referenceCount = null
            )
        } else if (isAttributionVisible) {
            SearchResultAttributionRow(
                languageCode = languageCode,
                contributorCount = contributorCount,
                referenceCount = referenceCount
            )
        }

        if (onRelevanceClick != null) {
            SearchResultRelevanceRow(
                modifier = Modifier.padding(top = 8.dp),
                languageCode = languageCode,
                relevance = relevance,
                onRelevanceClick = onRelevanceClick
            )
        }
    }
}

@Composable
private fun SearchResultAttributionRow(
    modifier: Modifier = Modifier,
    languageCode: String,
    contributorCount: Int?,
    referenceCount: Int?
) {
    val resources = LocalContext.current.getResources(languageCode)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        contributorCount?.let {
            SearchResultAttributionItem(
                iconRes = R.drawable.ic_group_24dp,
                text = resources.getQuantityString(R.plurals.semantic_search_result_contributors, it, it)
            )
        }
        referenceCount?.let {
            SearchResultAttributionItem(
                iconRes = R.drawable.ic_references_24dp,
                text = resources.getQuantityString(R.plurals.semantic_search_result_references, it, it)
            )
        }
    }
}

@Composable
private fun SearchResultAttributionItem(@DrawableRes iconRes: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            modifier = Modifier
                .padding(1.dp)
                .size(16.dp),
            contentDescription = null,
            tint = WikipediaTheme.colors.secondaryColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = WikipediaTheme.colors.secondaryColor
        )
    }
}

@Composable
private fun SearchResultRelevanceRow(
    modifier: Modifier = Modifier,
    languageCode: String,
    relevance: Boolean?,
    onRelevanceClick: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = context.getString(languageCode, R.string.search_result_relevance_question),
            style = MaterialTheme.typography.bodySmall,
            color = WikipediaTheme.colors.secondaryColor
        )
        listOf(true, false).forEach { isPositive ->
            val isSelected = relevance == isPositive
            IconButton(
                modifier = Modifier
                    .size(48.dp)
                    .semantics { selected = isSelected },
                onClick = { onRelevanceClick(isPositive) }
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(
                        when {
                            isPositive && isSelected -> R.drawable.ic_thumb_up_filled
                            isPositive -> R.drawable.ic_thumb_up
                            isSelected -> R.drawable.ic_thumb_down_filled
                            else -> R.drawable.ic_thumb_down
                        }
                    ),
                    contentDescription = context.getString(
                        languageCode,
                        if (isPositive) R.string.semantic_search_results_feedback_thumb_up_content_description
                        else R.string.semantic_search_results_feedback_thumb_down_content_description
                    ),
                    tint = WikipediaTheme.colors.primaryColor
                )
            }
        }
    }
}

private fun previewResult(
    languageCode: String = "en",
    title: String = "Communication",
    description: String? = "Transmission of information",
    type: SearchResult.SearchResultType = SearchResult.SearchResultType.SEMANTIC,
    sectionTitle: String? = "Definitions",
    text: String = "The word communication has its root in the Latin verb communicare, which means ' to share ' or ' to make common '. " +
            "Communication is usually understood as the transmission of information: a message is conveyed from a sender to a receiver " +
            "using some medium, such as sound, written signs, bodily movements, or electricity. Sender and receiver are often distinct " +
            "individuals but it is also possible for an individual to communicate with themselves.",
    thumbUrl: String? = "https://example",
    editCounts: Int? = 2348,
    referenceCounts: Int? = 35
): SearchResult {
    val pageTitle = PageTitle(title, WikiSite.preview(languageCode)).apply {
        this.description = description
        this.thumbUrl = thumbUrl
    }
    val isSemantic = type == SearchResult.SearchResultType.SEMANTIC
    return SearchResult(
        pageTitle = pageTitle,
        redirectFrom = null,
        type = type,
        snippet = text.takeIf { isSemantic },
        sectionTitle = sectionTitle,
        editCounts = editCounts,
        referenceCounts = referenceCounts,
        leadText = text.takeIf { !isSemantic }
    )
}

@Composable
private fun SearchResultCardPreviewList(
    theme: Theme,
    layoutDirection: LayoutDirection,
    results: List<SearchResult>,
    attributionLoading: Set<SearchResult> = emptySet()
) {
    BaseTheme(currentTheme = theme) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                results.forEach {
                    SearchResultCard(
                        searchResult = it,
                        isAttributionLoading = it in attributionLoading,
                        onRelevanceClick = {},
                        onItemClick = {}
                    )
                }
            }
        }
    }
}

// A semantic section match and a lexical lead match: same anatomy, with both trail forms.
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SearchResultCardPreview() {
    SearchResultCardPreviewList(
        Theme.LIGHT, LayoutDirection.Ltr, listOf(
            previewResult(),
            previewResult(
                title = "Communications satellite",
                description = "Artificial satellite that relays radio signals",
                type = SearchResult.SearchResultType.PREFIX,
                sectionTitle = null,
                text = "A communications satellite is an artificial satellite that relays and amplifies radio telecommunication signals via a transponder."
            )
        )
    )
}

// Line limits, no thumbnail with an empty lead (e.g. a disambiguation page), and counts still loading.
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SearchResultCardEdgeCasesPreview() {
    val loadingResult = previewResult(title = "Communication theory", editCounts = null, referenceCounts = null)
    SearchResultCardPreviewList(
        Theme.DARK, LayoutDirection.Ltr, listOf(
            previewResult(
                title = "Communication with 2 lines maximum and this is the limit of the title text",
                description = "Transmission of information with 2 lines maximum and this is the limit of this description",
                sectionTitle = "What if this is a really long title of a section and this is the limit which should be respected"
            ),
            previewResult(
                title = "Mercury",
                description = null,
                type = SearchResult.SearchResultType.PREFIX,
                sectionTitle = null,
                text = "",
                thumbUrl = null
            ),
            loadingResult
        ),
        attributionLoading = setOf(loadingResult)
    )
}

// Right-to-left layout. Arabic has no reference counts yet, so only contributors are shown.
@Preview(showBackground = true, widthDp = 360, locale = "ar")
@Composable
private fun SearchResultCardRtlPreview() {
    SearchResultCardPreviewList(
        Theme.LIGHT, LayoutDirection.Rtl, listOf(
            previewResult(
                languageCode = "ar",
                title = "اتصال (علم اجتماع)",
                description = "نقل المعلومات بين طرفين أو أكثر عبر وسيط",
                sectionTitle = "التعريفات والمفاهيم الأساسية في دراسات الاتصال الحديثة",
                text = "الاتصال هو عملية نقل المعلومات والأفكار والمشاعر بين الأفراد أو الجماعات من خلال وسائل مختلفة مثل الكلام والكتابة والإشارات.",
                referenceCounts = 12
            )
        )
    )
}
