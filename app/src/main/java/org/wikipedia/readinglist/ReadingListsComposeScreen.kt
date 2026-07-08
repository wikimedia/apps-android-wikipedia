package org.wikipedia.readinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
    searchQuery: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Reading Lists (Compose)",
            color = WikipediaTheme.colors.primaryColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (searchQuery.isNullOrEmpty()) "Compose rewrite scaffold — UI coming soon."
            else "Searching: \"$searchQuery\"",
            color = WikipediaTheme.colors.secondaryColor,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun ReadingListsComposeScreenPreview() {
    BaseTheme {
        ReadingListsComposeScreen(searchQuery = null)
    }
}
