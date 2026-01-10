package org.wikipedia.search

import android.content.Context
import android.location.Location
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.extensions.toAnnotatedStringWithBoldQuery
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil
import org.wikipedia.views.imageservice.ImageService

@Composable
fun SearchResultsScreen(
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
                    NoSearchResults(
                        countsPerLanguageCode = countsPerLanguageCode,
                        invokeSource = viewModel.invokeSource,
                        onLanguageClick = onLanguageClick
                    )
                }

                else -> {
                    SearchResultsList(
                        searchResultsPage = searchResults,
                        searchTerm = searchTerm.value,
                        onItemClick = onNavigateToTitle,
                        onItemLongClick = onItemLongClick,
                        semanticSearchConfig = SemanticSearchConfig(
                            isSemanticSearchExperimentOn = viewModel.isSemanticSearchExperimentOn,
                            onTitleClick = { searchResult ->
                                // TODO: navigate to deep search screen
                            },
                            onSuggestionTitleClick = { searchTerm ->
                                // TODO: navigate to deep search screen
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultsList(
    searchResultsPage: LazyPagingItems<SearchResult>,
    searchTerm: String?,
    onItemClick: (PageTitle, Boolean, Int, Location?) -> Unit,
    onItemLongClick: (View, SearchResult, Int) -> Unit,
    semanticSearchConfig: SemanticSearchConfig
) {
    if (semanticSearchConfig.isSemanticSearchExperimentOn) {
        SemanticSearchSuggestionView(
            modifier = Modifier.fillMaxSize(),
            searchResultsPage = searchResultsPage,
            semanticSearchConfig = semanticSearchConfig,
            searchTerm = searchTerm
        )
        return
    }

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
}

@Composable
fun SearchResultPageItem(
    searchResultPage: SearchResult,
    searchTerm: String?,
    onItemClick: () -> Unit,
    onItemLongClick: (View) -> Unit,
) {
    val (pageTitle, redirectFrom, type) = searchResultPage
    var anchorView by remember { mutableStateOf<View?>(null) }

    val isRedirect = !redirectFrom.isNullOrEmpty()

    val iconResId = when (type) {
        SearchResult.SearchResultType.HISTORY -> R.drawable.ic_history_24
        SearchResult.SearchResultType.TAB_LIST -> R.drawable.ic_tab_one_24px
        else -> R.drawable.ic_bookmark_border_white_24dp
    }

    val showImage =
        !pageTitle.thumbUrl.isNullOrEmpty()

    val boldenTitle = remember(pageTitle.displayText, searchTerm) {
        pageTitle.displayText.toAnnotatedStringWithBoldQuery(searchTerm)
    }

    val context = LocalContext.current

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onLongClick = {
                        anchorView?.let {
                            val imm =
                                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(it.windowToken, 0)
                            onItemLongClick(it)
                        }
                    },
                    onClick = {
                        onItemClick()
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                // Title with bold style
                Text(
                    text = boldenTitle,
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (isRedirect) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Image(
                            modifier = Modifier
                                .size(16.dp),
                            painter = painterResource(R.drawable.ic_subdirectory_arrow_right_black_24dp),
                            colorFilter = ColorFilter.tint(color = WikipediaTheme.colors.placeholderColor),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.search_redirect_from, redirectFrom),
                            color = WikipediaTheme.colors.secondaryColor,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 14.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    val description = pageTitle.description
                    if (!description.isNullOrEmpty()) {
                        Text(
                            modifier = Modifier
                                .padding(top = 2.dp),
                            text = description,
                            color = WikipediaTheme.colors.secondaryColor,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 14.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (type != SearchResult.SearchResultType.SEARCH) {
                    Image(
                        modifier = Modifier
                            .size(20.dp),
                        painter = painterResource(iconResId),
                        colorFilter = ColorFilter.tint(color = WikipediaTheme.colors.placeholderColor),
                        contentDescription = null
                    )
                }

                if (showImage) {
                    val request =
                        ImageService.getRequest(
                            LocalContext.current,
                            url = pageTitle.thumbUrl
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

        // invisible anchor view for LongPressMenu
        // using view based PopupMenu (vs pure Compose): Compose DropdownMenu does not have reliable
        // positioning with LazyColumn items and does not anchor properly with the parent layout.
        AndroidView(
            factory = { ctx ->
                View(ctx).apply {
                    anchorView = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .size(0.dp)
        )
    }
}
