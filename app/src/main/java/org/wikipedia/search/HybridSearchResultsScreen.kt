package org.wikipedia.search

import android.location.Location
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.UiState
import org.wikipedia.views.imageservice.ImageService

@Composable
fun HybridSearchResultsScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchResultsViewModel,
    onNavigateToTitle: (PageTitle, Boolean, Int, Location?) -> Unit,
    onSemanticItemClick: (PageTitle, Boolean, Int, Location?) -> Unit,
    onItemLongClick: (View, SearchResult, Int) -> Unit,
    onInfoClick: () -> Unit,
    onTurnOffExperimentClick: (String) -> Unit,
    onRatingClick: (Boolean) -> Unit,
    onCloseSearch: () -> Unit,
    onRetrySearch: () -> Unit,
    onLoading: (Boolean) -> Unit,
    onSemanticError: () -> Unit
) {
    val searchResultsState = viewModel.hybridSearchResultState.collectAsState().value
    val searchTerm = viewModel.searchTerm.collectAsState()

    val languageCode = viewModel.languageCode.collectAsState()
    val layoutDirection =
        if (L10nUtil.isLangRTL(languageCode.value)) LayoutDirection.Rtl else LayoutDirection.Ltr

    val isLoading = searchResultsState is UiState.Loading

    var showSearchProgressBar by remember { mutableStateOf(true) }
    LaunchedEffect(isLoading, showSearchProgressBar) {
        if (showSearchProgressBar) {
            onLoading(isLoading)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = modifier
        ) {
            when (searchResultsState) {
                is UiState.Loading -> {
                    showSearchProgressBar = false
                    HybridSearchSkeletonLoader(viewModel.getTestGroup)
                }

                is UiState.Success -> {
                    val semanticData = searchResultsState.data.filter { it.type == SearchResult.SearchResultType.SEMANTIC }
                    val lexicalData = searchResultsState.data.filter { it.type == SearchResult.SearchResultType.SEARCH }
                    if (semanticData.isEmpty()) {
                        onSemanticError()
                    }

                    HybridSearchResultsList(
                        testGroup = viewModel.getTestGroup,
                        searchResultsPage = lexicalData,
                        semanticSearchResultPage = semanticData,
                        searchTerm = searchTerm.value,
                        onItemClick = onNavigateToTitle,
                        onItemLongClick = onItemLongClick,
                        onInfoClick = onInfoClick,
                        onTurnOffExperimentClick = {
                            onTurnOffExperimentClick(searchTerm.value.orEmpty())
                        },
                        onSemanticItemClick = onSemanticItemClick,
                        onRatingClick = onRatingClick
                    )
                }

                is UiState.Error -> {
                    WikiErrorView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        caught = searchResultsState.error,
                        errorClickEvents = WikiErrorClickEvents(
                            backClickListener = { onCloseSearch() },
                            retryClickListener = { onRetrySearch() }
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HybridSearchResultsList(
    testGroup: String,
    searchResultsPage: List<SearchResult>,
    semanticSearchResultPage: List<SearchResult>,
    searchTerm: String?,
    onItemClick: (PageTitle, Boolean, Int, Location?) -> Unit,
    onItemLongClick: (View, SearchResult, Int) -> Unit,
    onInfoClick: () -> Unit,
    onTurnOffExperimentClick: () -> Unit,
    onSemanticItemClick: (PageTitle, Boolean, Int, Location?) -> Unit,
    onRatingClick: (Boolean) -> Unit
) {
    LazyColumn {
        if (testGroup == HybridSearchAbCTest.GROUP_CONTROL || testGroup == HybridSearchAbCTest.GROUP_LEXICAL_SEMANTIC) {
            items(
                count = searchResultsPage.size
            ) { index ->
                searchResultsPage[index].let { result ->
                    SearchResultPageItem(
                        searchResultPage = result,
                        searchTerm = searchTerm,
                        onItemClick = {
                            onItemClick(result.pageTitle, false, index, result.location)
                        },
                        onItemLongClick = { view ->
                            onItemLongClick(view, result, index)
                        }
                    )
                }
            }
            item {
                if (searchResultsPage.isNotEmpty() && semanticSearchResultPage.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        thickness = 1.dp,
                        color = WikipediaTheme.colors.borderColor
                    )
                }
            }
        }

        item {
            if (semanticSearchResultPage.isNotEmpty()) {
                SemanticSearchResultHeader(
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    onInfoClick = onInfoClick,
                    onTurnOffExperimentClick = onTurnOffExperimentClick
                )
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    count = semanticSearchResultPage.size
                ) { index ->
                    semanticSearchResultPage[index].let { result ->
                        SemanticSearchResultPageItem(
                            searchResult = result,
                            onSemanticItemClick = {
                                onSemanticItemClick(result.pageTitle, false, index, result.location)
                            },
                            onArticleItemClick = {
                                onItemClick(result.pageTitle, false, index, result.location)
                            },
                            onRatingClick = { isPositive ->
                                onRatingClick(isPositive)
                            }
                        )
                    }
                }
            }
        }

        if (testGroup == HybridSearchAbCTest.GROUP_SEMANTIC_LEXICAL) {
            item {
                if (semanticSearchResultPage.isNotEmpty() && searchResultsPage.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        thickness = 1.dp,
                        color = WikipediaTheme.colors.borderColor
                    )
                }
            }
            items(
                count = searchResultsPage.size
            ) { index ->
                searchResultsPage[index].let { result ->
                    SearchResultPageItem(
                        searchResultPage = result,
                        searchTerm = searchTerm,
                        onItemClick = {
                            onItemClick(result.pageTitle, false, index, result.location)
                        },
                        onItemLongClick = { view ->
                            onItemLongClick(view, result, index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SemanticSearchResultHeader(
    modifier: Modifier = Modifier,
    rephraseTitle: String? = null,
    onInfoClick: () -> Unit,
    onTurnOffExperimentClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current

    Column(
        modifier = modifier
    ) {
        if (!rephraseTitle.isNullOrEmpty()) {
            Text(
                modifier = Modifier
                    .padding(top = 16.dp),
                text = rephraseTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = WikipediaTheme.colors.primaryColor
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = WikipediaTheme.colors.progressiveColor,
                        shape = RoundedCornerShape(size = 16.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 1.dp)
            ) {
                Text(
                    text = stringResource(R.string.hybrid_search_beta_tag).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable {
                        DeviceUtil.hideSoftKeyboard(view)
                        expanded = true
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info_outline_black_24dp),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = stringResource(R.string.year_in_review_information_icon)
                )
                DropdownMenu(
                    expanded = expanded,
                    containerColor = WikipediaTheme.colors.paperColor,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.hybrid_search_onboarding_learn_more),
                                style = MaterialTheme.typography.bodyLarge,
                                color = WikipediaTheme.colors.primaryColor
                            ) },
                        onClick = onInfoClick
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.hybrid_search_turn_off_experiment_label),
                                style = MaterialTheme.typography.bodyLarge,
                                color = WikipediaTheme.colors.destructiveColor
                            ) },
                        onClick = onTurnOffExperimentClick
                    )
                }
            }
        }
        Text(
            modifier = Modifier.offset(y = (-8).dp),
            text = stringResource(R.string.hybrid_search_results_header_description),
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.placeholderColor
        )
    }
}

@Composable
fun SemanticSearchResultPageItem(
    searchResult: SearchResult,
    onSemanticItemClick: () -> Unit,
    onArticleItemClick: () -> Unit,
    onRatingClick: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .width(292.dp)
            .padding(bottom = 8.dp, top = 4.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        ),
        colors = CardDefaults.cardColors(containerColor = WikipediaTheme.colors.backgroundColor)
    ) {
        Column(
            modifier = Modifier.height(400.dp)
        ) {
            // TODO: need to check if the snippet is empty?
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onSemanticItemClick()
                    }
            ) {
                HtmlText(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    text = searchResult.snippet.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor,
                    linkStyle = TextLinkStyles(
                        style = SpanStyle(
                            color = WikipediaTheme.colors.progressiveColor,
                            fontSize = 16.sp
                        )
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var isRatingPositiveSelected by rememberSaveable(searchResult.pageTitle.prefixedText + "_positive") {
                    mutableStateOf(
                        false
                    )
                }
                var isRatingNegativeSelected by rememberSaveable(searchResult.pageTitle.prefixedText + "_negative") {
                    mutableStateOf(
                        false
                    )
                }
                val ratingLabel = if (isRatingPositiveSelected || isRatingNegativeSelected) {
                    stringResource(R.string.hybrid_search_results_rated_label)
                } else {
                    stringResource(R.string.hybrid_search_results_rate_label)
                }
                var isTextLaidOut by remember { mutableStateOf(false) }
                var isTextMultiline by remember { mutableStateOf(true) }
                Text(
                    modifier = if (isTextMultiline) {
                        Modifier.weight(1f).animateContentSize()
                    } else {
                        Modifier.wrapContentWidth().animateContentSize()
                    },
                    text = ratingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = WikipediaTheme.colors.placeholderColor,
                    onTextLayout = { result ->
                        if (!isTextLaidOut) {
                            val multilineNow = result.lineCount > 1
                            if (multilineNow != isTextMultiline) {
                                isTextMultiline = multilineNow
                            }
                            isTextLaidOut = true
                        }
                    }
                )
                AnimatedVisibility(
                    visible = !isRatingNegativeSelected,
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .animateContentSize()
                            .clip(CircleShape)
                            .clickable {
                                isRatingPositiveSelected = true
                                isTextLaidOut = false
                                onRatingClick(true)
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(16.dp)
                                .offset(y = (-2).dp),
                            painter = painterResource(if (isRatingPositiveSelected) R.drawable.ic_thumb_up_filled else R.drawable.ic_thumb_up),
                            contentDescription = stringResource(R.string.hybrid_search_results_rate_thumb_up),
                            tint = WikipediaTheme.colors.placeholderColor
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !isRatingPositiveSelected,
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .animateContentSize()
                            .clip(CircleShape)
                            .clickable {
                                isRatingNegativeSelected = true
                                isTextLaidOut = false
                                onRatingClick(false)
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(16.dp)
                                .offset(y = (-2).dp),
                            painter = painterResource(if (isRatingNegativeSelected) R.drawable.ic_thumb_down_filled else R.drawable.ic_thumb_down),
                            contentDescription = stringResource(R.string.hybrid_search_results_rate_thumb_down),
                            tint = WikipediaTheme.colors.placeholderColor
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.width(80.dp).padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = WikipediaTheme.colors.borderColor
            )

            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable {
                        onArticleItemClick()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.padding(end = 8.dp)
                            .weight(1f)
                    ) {
                        HtmlText(
                            text = searchResult.pageTitle.displayText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = WikipediaTheme.colors.primaryColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = searchResult.pageTitle.description.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WikipediaTheme.colors.placeholderColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SemanticSearchResultHeaderPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SemanticSearchResultHeader(
            rephraseTitle = "Who is Beyoncé?",
            onInfoClick = {},
            onTurnOffExperimentClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SemanticSearchResultPageItemPreview() {
    val wikiSite = WikiSite("en.wikipedia.org".toUri(), "en")
    val pageTitle = PageTitle("Beyoncé", wikiSite).apply {
        description = "American singer, songwriter, and actress"
        thumbUrl = "https://example"
    }
    val snippet = "Beyoncé Giselle Knowles-Carter is an <a href='#'>American singer</a>, songwriter, actress, and businesswoman. Born and raised in Houston, Texas, she performed in various singing and dancing competitions as a child. She rose to fame in the late 1990s as the lead singer of Destiny's Child, one of the world's best"

    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SemanticSearchResultPageItem(
            searchResult = SearchResult(
                pageTitle = pageTitle,
                searchResultType = SearchResult.SearchResultType.SEMANTIC,
                snippet = snippet
            ),
            onSemanticItemClick = {},
            onArticleItemClick = {},
            onRatingClick = {}
        )
    }
}
