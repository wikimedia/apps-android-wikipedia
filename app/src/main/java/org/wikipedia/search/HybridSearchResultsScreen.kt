package org.wikipedia.search

import android.location.Location
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import org.wikipedia.R
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.L10nUtil

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
        LazyColumn {
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
    results: List<SearchResult>
) {
    Column(
        modifier = Modifier
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
        Row {
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
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.hybrid_search_results_header_beta_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
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
            )
        )
    }
}
