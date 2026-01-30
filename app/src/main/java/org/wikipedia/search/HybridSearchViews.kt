package org.wikipedia.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import org.wikipedia.R
import org.wikipedia.compose.extensions.toAnnotatedStringWithBoldQuery
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun HybridSearchSuggestionView(
    modifier: Modifier = Modifier,
    searchResultsPage: LazyPagingItems<SearchResult>,
    hybridSearchConfig: HybridSearchConfig,
    searchTerm: String?,
) {
    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(top = 16.dp, bottom = 64.dp)
        ) {
            items(searchResultsPage.itemCount) { index ->
                searchResultsPage[index]?.let {
                    SearchResultTitleOnly(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = {
                                hybridSearchConfig.onTitleClick(it)
                            })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        searchResultPage = it,
                        searchTerm = searchTerm
                    )
                }
            }
        }

        SearchResultTitleOnlyBottomContent(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(WikipediaTheme.colors.paperColor)
                .clickable(
                    onClick = { hybridSearchConfig.onSuggestionTitleClick(searchTerm) }
                ),
            searchTerm = searchTerm
        )
    }
}

@Composable
fun SearchResultTitleOnly(
    searchResultPage: SearchResult,
    searchTerm: String?,
    modifier: Modifier = Modifier
) {
    val pageTitle = searchResultPage.pageTitle
    val boldenTitle = remember(pageTitle.displayText, searchTerm) {
        pageTitle.displayText.toAnnotatedStringWithBoldQuery(searchTerm)
    }
    Box(
        modifier = modifier
    ) {
        Text(
            text = boldenTitle,
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SearchResultTitleOnlyBottomContent(
    modifier: Modifier = Modifier,
    searchTerm: String?
) {
    val suggestionTitle = stringResource(R.string.hybrid_search_suggestion_title)
    Column(
        modifier = modifier
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(),
            color = WikipediaTheme.colors.borderColor,
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            text = buildAnnotatedString {
                append(suggestionTitle)
                withStyle(
                    style = SpanStyle(
                        color = WikipediaTheme.colors.progressiveColor
                    )
                ) {
                    append(" ")
                    append(searchTerm)
                }
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            ),
            color = WikipediaTheme.colors.primaryColor
        )
    }
}
