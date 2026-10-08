package org.wikipedia.search.semantic

import android.text.TextPaint
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getResources
import org.wikipedia.extensions.getString
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.theme.Theme
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.UiState
import org.wikipedia.views.imageservice.ImageService
import java.util.Date
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemanticSearchResultsScreen(
    modifier: Modifier = Modifier,
    viewModel: SemanticSearchResultsViewModel,
    onItemClick: (PageTitle) -> Unit,
    onCloseClick: () -> Unit,
    onFeedbackSubmit: (isPositive: Boolean?, feedbackText: String) -> Unit,
    onLoading: (Boolean) -> Unit,
) {

    val searchResultsState = viewModel.semanticSearchResultsState.collectAsState().value
    val feedbackState = viewModel.feedbackState.collectAsState().value

    val languageCode = viewModel.languageCode
    val layoutDirection =
        if (L10nUtil.isLangRTL(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr

    val interopConnection = rememberNestedScrollInteropConnection()
    val sheetDragState = rememberScrollableState { 0f }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .nestedScroll(interopConnection)
                .scrollable(
                    state = sheetDragState,
                    orientation = Orientation.Vertical
                )
        ) {

            BottomSheetDefaults.DragHandle(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                color = WikipediaTheme.colors.inactiveColor
            )

            SemanticSearchResultsHeader(
                languageCode = viewModel.languageCode,
                onCloseClick = onCloseClick
            )

            when (searchResultsState) {
                is UiState.Loading -> {
                    onLoading(true)
                    SemanticSearchResultsSkeletonLoader()
                }

                is UiState.Success -> {
                    onLoading(false)
                    val results = searchResultsState.data
                    if (results.isEmpty()) {
                        SemanticSearchNoResultsContent(
                            languageCode = viewModel.languageCode
                        )
                        return@CompositionLocalProvider
                    }
                    SemanticSearchFeedbackContent(
                        languageCode = viewModel.languageCode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(WikipediaTheme.colors.borderColor)
                            .verticalScroll(rememberScrollState()),
                        isVisible = feedbackState.isVisible,
                        selectedRating = feedbackState.isPositive,
                        onRatingClick = viewModel::selectFeedbackRating,
                        onSubmitClick = { isPositive, feedbackText ->
                            viewModel.submitFeedback()
                            onFeedbackSubmit(isPositive, feedbackText)
                        }
                    )
                    SemanticSearchResultsContent(
                        viewModel = viewModel,
                        items = results,
                        onItemClick = onItemClick
                    )
                }

                is UiState.Error -> {
                    onLoading(false)
                    SemanticSearchErrorContent(
                        throwable = searchResultsState.error,
                        wikiErrorClickEvents = WikiErrorClickEvents(
                            retryClickListener = {
                                viewModel.loadSemanticSearchResults()
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SemanticSearchResultsHeader(
    languageCode: String,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = WikipediaTheme.colors.borderColor,
                    shape = RoundedCornerShape(size = 16.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                modifier = Modifier,
                painter = painterResource(R.drawable.ic_experiment_24dp),
                tint = WikipediaTheme.colors.secondaryColor,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = context.getString(languageCode, R.string.semantic_search_beta_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = WikipediaTheme.colors.primaryColor
            )
        }

        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .size(48.dp)
                .offset(x = 12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close_black_24dp),
                contentDescription = context.getString(languageCode, R.string.semantic_search_results_close_button_content_description),
                tint = WikipediaTheme.colors.primaryColor
            )
        }
    }
}

@Composable
fun SemanticSearchFeedbackContent(
    modifier: Modifier,
    languageCode: String,
    isVisible: Boolean,
    selectedRating: Boolean?,
    isInputAlwaysVisible: Boolean = false,
    onRatingClick: (Boolean) -> Unit,
    onSubmitClick: (isPositive: Boolean?, feedbackText: String) -> Unit
) {
    val context = LocalContext.current
    val feedbackTextState = rememberTextFieldState()
    val focusManager = LocalFocusManager.current

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = context.getString(languageCode, R.string.semantic_search_results_feedback_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
                SemanticSearchFeedbackRatingButton(
                    iconRes = R.drawable.ic_thumb_up,
                    selectedIconRes = R.drawable.ic_thumb_up_filled,
                    contentDescription = context.getString(languageCode, R.string.semantic_search_results_feedback_thumb_up_content_description),
                    isSelected = selectedRating == true,
                    onClick = { onRatingClick(true) }
                )
                SemanticSearchFeedbackRatingButton(
                    iconRes = R.drawable.ic_thumb_down,
                    selectedIconRes = R.drawable.ic_thumb_down_filled,
                    contentDescription = context.getString(languageCode, R.string.semantic_search_results_feedback_thumb_down_content_description),
                    isSelected = selectedRating == false,
                    onClick = { onRatingClick(false) }
                )
            }

            AnimatedVisibility(
                visible = isInputAlwaysVisible || selectedRating != null
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = WikipediaTheme.colors.paperColor),
                        state = feedbackTextState,
                        inputTransformation = InputTransformation.maxLength(230), // required by the API
                        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 4),
                        shape = RoundedCornerShape(4.dp),
                        placeholder = {
                            Text(
                                text = context.getString(languageCode, R.string.semantic_search_results_feedback_input_hint)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WikipediaTheme.colors.primaryColor,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = WikipediaTheme.colors.primaryColor,
                            errorTextColor = WikipediaTheme.colors.primaryColor
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AppButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        enabled = selectedRating != null,
                        onClick = {
                            focusManager.clearFocus()
                            onSubmitClick(selectedRating, feedbackTextState.text.toString().trim())
                        }
                    ) {
                        Text(
                            text = context.getString(languageCode, R.string.semantic_search_results_feedback_submit_button_text),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SemanticSearchFeedbackRatingButton(
    @DrawableRes iconRes: Int,
    @DrawableRes selectedIconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        modifier = Modifier.semantics { selected = isSelected },
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(if (isSelected) selectedIconRes else iconRes),
            contentDescription = contentDescription,
            tint = WikipediaTheme.colors.primaryColor
        )
    }
}

@Composable
fun SemanticSearchNoResultsContent(
    languageCode: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.illustration_no_results),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = context.getString(languageCode, R.string.semantic_search_no_results_message),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SemanticSearchErrorContent(
    throwable: Throwable?,
    wikiErrorClickEvents: WikiErrorClickEvents
) {
    WikiErrorView(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        caught = throwable,
        errorClickEvents = wikiErrorClickEvents,
        retryForGenericError = true
    )
}

@Composable
fun SemanticSearchResultsContent(
    modifier: Modifier = Modifier,
    viewModel: SemanticSearchResultsViewModel,
    items: List<SearchResult>,
    onItemClick: (PageTitle) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor)
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(items.size, key = { items[it].pageTitle }) { index ->
                val searchResult = items[index]
                SemanticSearchResultCard(
                    languageCode = viewModel.languageCode,
                    prefixQuotationMark = SemanticSearchHelper.getQuotationMark(viewModel.languageCode),
                    showLastUpdatedTime = viewModel.languageCode == "ar",
                    searchResult = searchResult,
                    onItemClick = { onItemClick(searchResult.pageTitle) },
                    onLinkClick = { url ->
                        // ignore in-article links
                    }
                )
            }
        }
    }
}

@Composable
fun SemanticSearchResultCard(
    modifier: Modifier = Modifier,
    languageCode: String,
    prefixQuotationMark: String,
    showLastUpdatedTime: Boolean,
    searchResult: SearchResult,
    onItemClick: (() -> Unit)? = null,
    showReadInArticleText: Boolean = true,
    onLinkClick: (String) -> Unit
) {
    val context = LocalContext.current
    val articlePath = listOfNotNull(
        searchResult.pageTitle.displayText.takeIf { it.isNotBlank() },
        searchResult.sectionTitle?.takeIf { it.isNotBlank() }
    ).joinToString(" | ")

    val editCount = searchResult.editCounts ?: 0
    val referenceCounts = searchResult.referenceCounts ?: 0
    val lastUpdatedDate = searchResult.lastUpdated?.let { DateUtil.iso8601DateParse(it) } ?: Date()
    WikiCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.backgroundColor,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        ),
        elevation = 0.dp,
        onClick = onItemClick
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val quotationMarkFontSize = 24.sp
                val snippetFontSize = 14.sp
                Text(
                    modifier = Modifier.offset(y = (-8).dp),
                    text = prefixQuotationMark,
                    fontSize = quotationMarkFontSize,
                    color = WikipediaTheme.colors.primaryColor
                )
                HtmlText(
                    text = leadingSpacesForQuotationMark(
                        quotationMark = prefixQuotationMark,
                        quoteTextSize = quotationMarkFontSize,
                        contentTextSize = snippetFontSize,
                        reserveGap = 4.dp
                    ) + searchResult.snippet.orEmpty(),
                    style = TextStyle(
                        color = WikipediaTheme.colors.primaryColor,
                        fontSize = snippetFontSize
                    ),
                    color = WikipediaTheme.colors.primaryColor,
                    linkStyle = TextLinkStyles(
                        style = SpanStyle(
                            color = WikipediaTheme.colors.progressiveColor,
                            fontSize = snippetFontSize
                        )
                    ),
                    linkInteractionListener = {
                        val url = (it as LinkAnnotation.Url).url
                        onLinkClick(url)
                    },
                    maxLines = 8
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (showReadInArticleText) {
                Text(
                    text = context.getString(languageCode, R.string.semantic_search_results_read_in_article_label),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = WikipediaTheme.colors.secondaryColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!searchResult.pageTitle.thumbUrl.isNullOrEmpty()) {
                    val request =
                        ImageService.getRequest(
                            LocalContext.current,
                            url = searchResult.pageTitle.thumbUrl
                        )
                    AsyncImage(
                        model = request,
                        placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                        error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                HtmlText(
                    text = articlePath,
                    style = TextStyle(
                        color = WikipediaTheme.colors.primaryColor,
                        fontSize = 12.sp
                    ),
                    color = WikipediaTheme.colors.primaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                color = WikipediaTheme.colors.borderColor,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_group_24dp),
                        modifier = Modifier.size(15.dp),
                        contentDescription = null,
                        tint = WikipediaTheme.colors.secondaryColor,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = context.getResources(languageCode).getQuantityString(R.plurals.semantic_search_result_contributors, editCount, editCount),
                        fontSize = 12.sp,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val itemIcon = if (showLastUpdatedTime) R.drawable.ic_schedule_24dp else
                        R.drawable.ic_references_24dp
                    val itemText = if (showLastUpdatedTime) context.getString(languageCode, R.string.semantic_search_results_last_updated_label,
                        DateUtil.getMonthWithYearString(lastUpdatedDate)) else
                        context.getResources(languageCode).getQuantityString(R.plurals.semantic_search_result_references, referenceCounts, referenceCounts)
                    Icon(
                        painter = painterResource(itemIcon),
                        modifier = Modifier.size(15.dp),
                        contentDescription = null,
                        tint = WikipediaTheme.colors.secondaryColor,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = itemText,
                        fontSize = 12.sp,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun leadingSpacesForQuotationMark(
    quotationMark: String,
    quoteTextSize: androidx.compose.ui.unit.TextUnit = 32.sp,
    contentTextSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    reserveGap: Dp = 4.dp
): String {
    val density = LocalDensity.current

    val quotePaint = remember(quoteTextSize, density) {
        TextPaint().apply { textSize = with(density) { quoteTextSize.toPx() } }
    }
    val contentPaint = remember(contentTextSize, density) {
        TextPaint().apply { textSize = with(density) { contentTextSize.toPx() } }
    }

    val quoteWidthPx = quotePaint.measureText(quotationMark).coerceAtLeast(0f)
    val spaceWidthPx = contentPaint.measureText("\u00A0").coerceAtLeast(1f)
    val targetWidthPx = quoteWidthPx + with(density) { reserveGap.toPx() }
    val count = ceil(targetWidthPx / spaceWidthPx).toInt().coerceAtLeast(0)

    return "\u00A0".repeat(count)
}

@Preview(showBackground = true)
@Composable
fun SemanticSearchResultCardPreview() {
    val wikiSite = WikiSite.preview()
    val pageTitle = PageTitle("Beyoncé", wikiSite).apply {
        description = "American singer, songwriter, and actress"
        thumbUrl = "https://example"
    }
    val snippet = "Beyoncé Giselle Knowles-Carter is an <a href='#'>American singer</a>, songwriter, actress, and <span class=\"searchmatch\">businesswoman</span>. Born and raised in Houston, Texas, she performed in various singing and dancing competitions as a child. She rose to fame in the late 1990s as the lead singer of Destiny's Child, one of the world's best"

    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SemanticSearchResultCard(
            languageCode = wikiSite.languageCode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            prefixQuotationMark = "«",
            showLastUpdatedTime = true,
            searchResult = SearchResult(
                pageTitle = pageTitle,
                searchResultType = SearchResult.SearchResultType.SEMANTIC,
                snippet = snippet
            ),
            onItemClick = {},
            onLinkClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SemanticSearchFeedbackContentPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SemanticSearchFeedbackContent(
            modifier = Modifier,
            languageCode = "en",
            isVisible = true,
            selectedRating = true,
            onRatingClick = {},
            onSubmitClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SemanticSearchNoResultsPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SemanticSearchNoResultsContent(
            languageCode = "en",
            modifier = Modifier
        )
    }
}
