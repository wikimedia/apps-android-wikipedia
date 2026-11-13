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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.sp
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

private const val TITLE_MAX_LINES = 2
private const val DESCRIPTION_MAX_LINES = 2
private const val TRAIL_MAX_LINES = 2
private const val EXCERPT_MAX_LINES = 8

@Composable
fun SearchResultCard(
    model: SearchResultCardUiModel,
    modifier: Modifier = Modifier,
    relevance: Boolean? = null,
    onRelevanceClick: ((Boolean) -> Unit)? = null,
    onItemClick: () -> Unit
) {
    val context = LocalContext.current
    val languageCode = model.languageCode
    val trail = model.trail ?: context.getString(languageCode, R.string.search_result_trail_lead)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WikipediaTheme.colors.backgroundColor)
            .clickable(onClick = onItemClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (onRelevanceClick != null) 4.dp else 16.dp)
    ) {
        // Header: title, description and source trail, with the thumbnail beside them when there is one.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.title,
                    color = WikipediaTheme.colors.primaryColor,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
                model.description?.let {
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = it,
                        color = WikipediaTheme.colors.secondaryColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = DESCRIPTION_MAX_LINES,
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
                        text = trail,
                        color = WikipediaTheme.colors.primaryColor,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = TRAIL_MAX_LINES,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            model.thumbUrl?.let { thumbUrl ->
                AsyncImage(
                    model = ImageService.getRequest(context, url = thumbUrl),
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

        if (model.excerpt.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = WikipediaTheme.colors.borderColor,
                thickness = 1.dp
            )
            Text(
                text = model.excerpt,
                color = WikipediaTheme.colors.primaryColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = EXCERPT_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = context.getString(languageCode, R.string.semantic_search_results_read_in_article_label),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = WikipediaTheme.colors.secondaryColor
            )
        }

        if (model.isAttributionLoading) {
            // Keep the row's space while counts load, so the card doesn't grow when they arrive.
            Spacer(modifier = Modifier.height(16.dp))
            SearchResultAttributionRow(
                modifier = Modifier
                    .alpha(0f)
                    .clearAndSetSemantics {},
                languageCode = languageCode,
                contributorCount = 0,
                referenceCount = null
            )
        } else if (model.isAttributionVisible) {
            Spacer(modifier = Modifier.height(16.dp))
            SearchResultAttributionRow(
                languageCode = languageCode,
                contributorCount = model.contributorCount,
                referenceCount = model.referenceCount
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
            modifier = Modifier.size(15.dp),
            contentDescription = null,
            tint = WikipediaTheme.colors.secondaryColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
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
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = context.getString(languageCode, R.string.search_result_relevance_question),
            fontSize = 12.sp,
            color = WikipediaTheme.colors.secondaryColor
        )
        listOf(true, false).forEach { isPositive ->
            val isSelected = relevance == isPositive
            IconButton(
                modifier = Modifier.semantics { selected = isSelected },
                onClick = { onRelevanceClick(isPositive) }
            ) {
                Icon(
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
    val pageTitle = PageTitle(title, WikiSite.forLanguageCode(languageCode)).apply {
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
    models: List<SearchResultCardUiModel>
) {
    BaseTheme(currentTheme = theme) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                models.forEach {
                    SearchResultCard(
                        model = it,
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
            previewResult().toCardUiModel(),
            previewResult(
                title = "Communications satellite",
                description = "Artificial satellite that relays radio signals",
                type = SearchResult.SearchResultType.PREFIX,
                sectionTitle = null,
                text = "A communications satellite is an artificial satellite that relays and amplifies radio telecommunication signals via a transponder."
            ).toCardUiModel()
        )
    )
}

// Line limits, no thumbnail with an empty lead (e.g. a disambiguation page), and counts still loading.
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SearchResultCardEdgeCasesPreview() {
    SearchResultCardPreviewList(
        Theme.DARK, LayoutDirection.Ltr, listOf(
            previewResult(
                title = "Communication with 2 lines maximum and this is the limit of the title text",
                description = "Transmission of information with 2 lines maximum and this is the limit of this description",
                sectionTitle = "What if this is a really long title of a section and this is the limit which should be respected"
            ).toCardUiModel(),
            previewResult(
                title = "Mercury",
                description = null,
                type = SearchResult.SearchResultType.PREFIX,
                sectionTitle = null,
                text = "",
                thumbUrl = null
            ).toCardUiModel(),
            previewResult(editCounts = null, referenceCounts = null).toCardUiModel(isAttributionLoading = true)
        )
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
            ).toCardUiModel()
        )
    )
}
