package org.wikipedia.search

import android.location.Location
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
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
import org.wikipedia.util.L10nUtil
import org.wikipedia.views.imageservice.ImageService

@Composable
fun HybridSearchResultsScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchResultsViewModel,
    onNavigateToTitle: (PageTitle, Boolean, Int, Location?) -> Unit,
    onItemLongClick: (View, SearchResult, Int) -> Unit,
    onLanguageClick: (Int) -> Unit,
    onCloseSearch: () -> Unit,
    onRetrySearch: () -> Unit,
    onLoading: (Boolean) -> Unit
) {
    val searchResults = viewModel.searchResultsFlow.collectAsLazyPagingItems()
    val searchTerm = viewModel.searchTerm.collectAsState()
    val loadState = searchResults.loadState
    val countsPerLanguageCode = viewModel.countsPerLanguageCode

    val languageCode = viewModel.languageCode.collectAsState()
    val layoutDirection =
        if (L10nUtil.isLangRTL(languageCode.value.orEmpty())) LayoutDirection.Rtl else LayoutDirection.Ltr

    // this is a callback to show loading indicator in the SearchFragment.
    // It is placed outside the UI logic to prevent flickering. We need to show the loader both initial load (refresh) and pagination (append) without hiding the list or conflicting with other UI states.

    val isLoading = loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading
    LaunchedEffect(isLoading) {
        onLoading(isLoading)
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = modifier
        ) {
            when {
                loadState.refresh is LoadState.Loading -> {} // when offline prevents UI from loading old list

                loadState.refresh is LoadState.Error -> {
                    val error = (loadState.refresh as LoadState.Error).error
                    WikiErrorView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        caught = error,
                        errorClickEvents = WikiErrorClickEvents(
                            backClickListener = { onCloseSearch() },
                            retryClickListener = { onRetrySearch() }
                        )
                    )
                }

                loadState.append is LoadState.NotLoading && loadState.append.endOfPaginationReached && searchResults.itemCount == 0 -> {
                    // TODO: verify the empty state
                    NoSearchResults(
                        countsPerLanguageCode = countsPerLanguageCode,
                        invokeSource = viewModel.invokeSource,
                        onLanguageClick = onLanguageClick
                    )
                }

                else -> {
                    // TODO: hybrid search: two lazy columns
                    HybridSearchResultsList(
                        searchResultsPage = searchResults,
                        searchTerm = searchTerm.value,
                        onItemClick = onNavigateToTitle,
                        onItemLongClick = onItemLongClick
                    )
                }
            }
        }
    }
}

@Composable
fun HybridSearchResultsList(
    searchResultsPage: LazyPagingItems<SearchResult>,
    searchTerm: String?,
    onItemClick: (PageTitle, Boolean, Int, Location?) -> Unit,
    onItemLongClick: (View, SearchResult, Int) -> Unit,
) {

    val scrollState = rememberScrollState()
    // TODO: handle the B and C tests here.
    Column(
        modifier = Modifier
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Standard search results list
        LazyColumn {
            items(
                count = searchResultsPage.itemCount
            ) { index ->
                searchResultsPage[index]?.let { result ->
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

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        }

        // Semantic search results list - horizontally scrolling list
        LazyRow {
            items(
                count = searchResultsPage.itemCount
            ) { index ->
//            searchResultsPage[index]?.let { result ->
//                SearchResultPageItem(
//                    searchResultPage = result,
//                    searchTerm = searchTerm,
//                    onItemClick = {
//                        onItemClick(result.pageTitle, false, index, result.location)
//                    },
//                    onItemLongClick = { view ->
//                        onItemLongClick(view, result, index)
//                    }
//                )
//            }
            }
        }
    }
}

@Composable
fun SemanticSearchResultHeader(
    modifier: Modifier = Modifier,
    rephraseTitle: String? = null,
    results: List<SearchResult>,
    onInfoClick: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        if (!rephraseTitle.isNullOrEmpty()) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = rephraseTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val resultsCount = results.size
            val articlesCount = results.distinctBy { it.pageTitle.prefixedText }.size
            val headerText = stringResource(R.string.hybrid_search_results_header,
                pluralStringResource(R.plurals.hybrid_search_results_header_result, resultsCount, resultsCount),
                pluralStringResource(R.plurals.hybrid_search_results_header_article, articlesCount, articlesCount)
            )
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = headerText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Box(
                modifier = Modifier
                    .background(
                        color = WikipediaTheme.colors.progressiveColor,
                        shape = RoundedCornerShape(size = 16.dp)
                    )
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.hybrid_search_results_header_beta_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
            }
            Icon(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clickable {
                        onInfoClick()
                    },
                painter = painterResource(R.drawable.ic_info_outline_black_24dp),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(R.string.year_in_review_information_icon)
            )
        }
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
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        ),
        colors = CardDefaults.cardColors(containerColor = WikipediaTheme.colors.backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // TODO: need to check if the extract is empty?
            HtmlText(
                modifier = Modifier.clickable {
                        onSemanticItemClick()
                    },
                linkStyle = TextLinkStyles(
                    style = SpanStyle(
                        color = WikipediaTheme.colors.progressiveColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                ),
                text = buildString {
                    append(searchResult.pageTitle.extract.orEmpty())
                    append("…")
                    append("<a href='#'><b>${stringResource(R.string.hybrid_search_results_more_button).lowercase()}</b></a>")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.hybrid_search_results_rate_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = WikipediaTheme.colors.placeholderColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    modifier = Modifier.size(16.dp)
                        .clickable {
                            onRatingClick(true)
                        },
                    painter = painterResource(R.drawable.ic_thumb_up),
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(24.dp))
                Icon(
                    modifier = Modifier.size(16.dp)
                        .clickable {
                            onRatingClick(false)
                        },
                    painter = painterResource(R.drawable.ic_thumb_down),
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.width(48.dp),
                thickness = 0.5.dp,
                color = WikipediaTheme.colors.borderColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onArticleItemClick()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    HtmlText(
                        text = searchResult.pageTitle.displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Text(
                        text = searchResult.pageTitle.description.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.placeholderColor
                    )
                }
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

@Preview(showBackground = true)
@Composable
private fun SemanticSearchResultHeaderPreview() {
    val wikiSite = WikiSite("en.wikipedia.org".toUri(), "en")
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SemanticSearchResultHeader(
            rephraseTitle = "Who is Beyoncé?",
            results = listOf(
                SearchResult(PageTitle("Beyoncé", wikiSite), SearchResult.SearchResultType.SEMANTIC),
                SearchResult(PageTitle("Beyoncé", wikiSite), SearchResult.SearchResultType.SEMANTIC),
                SearchResult(PageTitle("Beyoncé Knowles", wikiSite), SearchResult.SearchResultType.SEMANTIC),
                SearchResult(PageTitle("Beyoncé (album)", wikiSite), SearchResult.SearchResultType.SEMANTIC)
            ),
            onInfoClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SemanticSearchResultPageItemPreview() {
    val wikiSite = WikiSite("en.wikipedia.org".toUri(), "en")
    val pageTitle = PageTitle("Beyoncé", wikiSite).apply {
        description = "American singer, songwriter, and actress"
        extract =
            "Beyoncé Giselle Knowles-Carter is an <a href='#'>American singer</a>, songwriter, actress, and businesswoman. Born and raised in Houston, Texas, she performed in various singing and dancing competitions as a child. She rose to fame in the late 1990s as the lead singer of Destiny's Child, one of the world's best"
    }
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SemanticSearchResultPageItem(
            searchResult = SearchResult(pageTitle, SearchResult.SearchResultType.SEMANTIC),
            onSemanticItemClick = {},
            onArticleItemClick = {},
            onRatingClick = {}
        )
    }
}
