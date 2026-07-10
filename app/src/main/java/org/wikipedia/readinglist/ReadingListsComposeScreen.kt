package org.wikipedia.readinglist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
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
        is ReadingListsUiState.Content.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize()
            ) {
                items(
                    items = content.rows,
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
                        // TODO migration: replace with the PageRow composable (search-result article row).
                        is ReadingListRow.PageRow -> Text(
                            text = "– ${row.page.title}  [in: ${row.containingLists.joinToString()}]",
                            color = WikipediaTheme.colors.progressiveColor
                        )
                    }
                    HorizontalDivider(
                        color = WikipediaTheme.colors.borderColor,
                        thickness = 0.5.dp
                    )
                }
            }
        }
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
