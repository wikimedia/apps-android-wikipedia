package org.wikipedia.readinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun ReadingListsComposeScreen(
    uiState: ReadingListsUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Reading Lists (Compose)",
            color = WikipediaTheme.colors.primaryColor,
            textAlign = TextAlign.Center
        )

        // TODO migration: temporary UI to verify data retrieval.
        when (val content = uiState.content) {
            is ReadingListsUiState.Content.Loading -> {
                Text(
                    text = "Loading…",
                    color = WikipediaTheme.colors.secondaryColor,
                    textAlign = TextAlign.Center
                )
            }
            is ReadingListsUiState.Content.Success -> {
                Text(
                    text = "Loaded ${content.rows.size} row(s)",
                    color = WikipediaTheme.colors.secondaryColor,
                    textAlign = TextAlign.Center
                )
                content.rows.forEach { row ->
                    when (row) {
                        is ReadingListRow.ListRow -> Text(
                            text = "• ${row.list.title} (${row.list.numPages})",
                            color = WikipediaTheme.colors.primaryColor,
                            textAlign = TextAlign.Center
                        )
                        is ReadingListRow.PageRow -> Text(
                            text = "– ${row.page.title}  [in: ${row.containingLists.joinToString()}]",
                            color = WikipediaTheme.colors.progressiveColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is ReadingListsUiState.Content.Error -> {
                Text(
                    text = "Error: ${content.throwable.message}",
                    color = WikipediaTheme.colors.destructiveColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (!uiState.searchQuery.isNullOrEmpty()) {
            Text(
                text = "Searching: \"${uiState.searchQuery}\"",
                color = WikipediaTheme.colors.secondaryColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun ReadingListsComposeScreenPreview() {
    BaseTheme {
        ReadingListsComposeScreen(
            uiState = ReadingListsUiState(
                content = ReadingListsUiState.Content.Success(
                    listOf(
                        ReadingListRow.ListRow(ReadingListUiModel(id = 1, title = "Default", description = null, isDefault = true, numPages = 3, sizeBytes = 0)),
                        ReadingListRow.ListRow(ReadingListUiModel(id = 2, title = "Physics", description = "reading", isDefault = false, numPages = 12, sizeBytes = 0))
                    )
                )
            )
        )
    }
}
