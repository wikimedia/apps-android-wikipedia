package org.wikipedia.readinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.components.SearchEmptyView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.compose.ReadingListPageRow
import org.wikipedia.readinglist.compose.ReadingListRow
import org.wikipedia.theme.Theme

@Composable
fun ReadingListsComposeScreen(
    uiState: ReadingListsUiState,
    modifier: Modifier = Modifier
) {
    when (val content = uiState.content) {
        is ReadingListsUiState.Content.Loading -> {
            // TODO migration: show loading indicator
        }
        is ReadingListsUiState.Content.Error -> {
            // TODO migration: show error message
        }
        is ReadingListsUiState.Content.Success -> when {
            content.rows.isEmpty() && uiState.searchQuery.isNullOrEmpty() -> {
                EmptyReadingLists(modifier = modifier)
            }
            content.rows.isEmpty() -> {
                SearchEmptyView(
                    modifier = modifier.fillMaxSize(),
                    emptyTexTitle = stringResource(R.string.search_reading_lists_no_results)
                )
            }
            else -> {
                ReadingListsList(rows = content.rows, modifier = modifier)
            }
        }
    }
}

@Composable
private fun ReadingListsList(
    rows: List<ReadingListRow>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(
            items = rows,
            key = { row ->
                when (row) {
                    is ReadingListRow.ListRow -> "list-${row.list.id}"
                    is ReadingListRow.PageRow -> "page-${row.page.id}"
                }
            }
        ) { row ->
            when (row) {
                is ReadingListRow.ListRow -> ReadingListRow(
                    list = row.list,
                    onClick = {},
                    onLongClick = {}
                )
                is ReadingListRow.PageRow -> ReadingListPageRow(
                    page = row.page,
                    containingLists = row.containingLists,
                    onClick = {},
                    onLongClick = {}
                )
            }
            HorizontalDivider(
                color = WikipediaTheme.colors.borderColor,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun EmptyReadingLists(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.saved_list_empty_title),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.reading_lists_empty_message),
            color = WikipediaTheme.colors.secondaryColor,
            style = MaterialTheme.typography.bodyLarge.copy(
                letterSpacing = 0.15.sp,
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun ReadingListsComposeScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ReadingListsComposeScreen(
            uiState = ReadingListsUiState(
                content = ReadingListsUiState.Content.Success(
                    listOf(
                        ReadingListRow.ListRow(ReadingListUiModel(id = 1, title = "Default", description = null, isDefault = true, totalPages = 3, sizeBytesFromPages = 0)),
                        ReadingListRow.ListRow(ReadingListUiModel(id = 2, title = "Physics", description = "reading", isDefault = false, totalPages = 12, sizeBytesFromPages = 1240000))
                    )
                )
            )
        )
    }
}

@Preview
@Composable
private fun ReadingListsEmptyPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ReadingListsComposeScreen(
            uiState = ReadingListsUiState(content = ReadingListsUiState.Content.Success(emptyList()))
        )
    }
}
