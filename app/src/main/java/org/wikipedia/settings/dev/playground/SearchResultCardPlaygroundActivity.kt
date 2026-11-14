package org.wikipedia.settings.dev.playground

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.search.interleave.SearchResultCard
import org.wikipedia.search.interleave.toCardUiModel
import org.wikipedia.theme.Theme
import org.wikipedia.util.L10nUtil

// Shows the US-2 search result card with sample results for each target wiki, so QA and design can
// check it against Figma before the card is wired into search (US-1).
class SearchResultCardPlaygroundActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                SearchResultCardPlaygroundScreen(
                    initialTheme = WikipediaApp.instance.currentTheme,
                    onBackClick = { finish() }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SearchResultCardPlaygroundActivity::class.java)
        }
    }
}

@Composable
fun SearchResultCardPlaygroundScreen(
    initialTheme: Theme,
    onBackClick: () -> Unit
) {
    var languageCode by remember { mutableStateOf(SearchResultCardPlaygroundSamples.languageCodes.first()) }
    var theme by remember { mutableStateOf(initialTheme) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            WikiTopAppBar(
                title = "Search result cards",
                onNavigationClick = onBackClick
            )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                text = "**Sample content for layout reference only. These are not live search results, and the article text and figures are illustrative.",
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.secondaryColor
            )
            PlaygroundChipRow(
                label = "Wiki",
                options = SearchResultCardPlaygroundSamples.languageCodes,
                selected = languageCode,
                optionLabel = { it },
                onSelect = { languageCode = it }
            )
            PlaygroundChipRow(
                label = "Theme",
                options = Theme.entries,
                selected = theme,
                optionLabel = { stringResource(it.nameId) },
                onSelect = { theme = it }
            )

            // The cards use the selected theme and the wiki's text direction, like the real search list.
            BaseTheme(currentTheme = theme) {
                val layoutDirection = if (L10nUtil.isLangRTL(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    SearchResultCardPlaygroundList(languageCode = languageCode)
                }
            }
        }
    }
}

@Composable
private fun SearchResultCardPlaygroundList(languageCode: String) {
    val models = remember(languageCode) {
        SearchResultCardPlaygroundSamples.results(languageCode).map {
            it.toCardUiModel(isAttributionLoading = it.pageId in SearchResultCardPlaygroundSamples.attributionLoadingPageIds)
        }
    }
    // Votes are kept only for this wiki's list, so switching wikis clears them.
    val votes = remember(languageCode) { mutableStateMapOf<Int, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(models, key = { it.pageId }) { model ->
            SearchResultCard(
                model = model,
                relevance = votes[model.pageId],
                onRelevanceClick = { votes[model.pageId] = it },
                onItemClick = {}
            )
        }
    }
}

@Composable
private fun <T> PlaygroundChipRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = WikipediaTheme.colors.secondaryColor
        )
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(optionLabel(option)) }
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun SearchResultCardPlaygroundScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        SearchResultCardPlaygroundScreen(initialTheme = Theme.LIGHT, onBackClick = {})
    }
}
