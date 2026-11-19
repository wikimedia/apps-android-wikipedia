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
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.search.interleave.InterleaveSearchAbTest
import org.wikipedia.search.interleave.SearchResultCard
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil

class SearchResultCardPlaygroundActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                SearchResultCardPlaygroundScreen(
                    initialTheme = WikipediaApp.instance.currentTheme,
                    onBackClick = { finish() },
                    onFeedbackSubmit = {
                        FeedbackUtil.showMessage(this, R.string.semantic_search_results_feedback_submitted_message)
                    }
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
    wikiSiteFor: (String) -> WikiSite = { WikiSite.forLanguageCode(it) },
    onBackClick: () -> Unit,
    onFeedbackSubmit: () -> Unit = {}
) {
    var languageCode by remember { mutableStateOf(SearchResultCardPlaygroundSamples.languageCodes.first()) }
    var theme by remember { mutableStateOf(initialTheme) }
    var feedbackMode by remember { mutableStateOf(PlaygroundFeedbackMode.ALWAYS) }

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
            PlaygroundChipRow(
                label = "Feedback",
                options = PlaygroundFeedbackMode.entries,
                selected = feedbackMode,
                optionLabel = { it.label },
                onSelect = { feedbackMode = it }
            )

            // The cards use the selected theme and the wiki's text direction, like the real search list.
            BaseTheme(currentTheme = theme) {
                val layoutDirection = if (L10nUtil.isLangRTL(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    SearchResultCardPlaygroundList(
                        languageCode = languageCode,
                        feedbackMode = feedbackMode,
                        wikiSiteFor = wikiSiteFor,
                        onFeedbackSubmit = onFeedbackSubmit
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCardPlaygroundList(
    languageCode: String,
    feedbackMode: PlaygroundFeedbackMode,
    wikiSiteFor: (String) -> WikiSite,
    onFeedbackSubmit: () -> Unit
) {
    val results = remember(languageCode) { SearchResultCardPlaygroundSamples.results(languageCode, wikiSiteFor(languageCode)) }
    // Like a real results list, the daily check runs once per list, so switching wikis counts as a new list.
    val isFeedbackShown = remember(languageCode, feedbackMode) {
        val abTest = InterleaveSearchAbTest()
        when {
            feedbackMode == PlaygroundFeedbackMode.ALWAYS -> true
            abTest.shouldShowDailyPrompt() -> {
                abTest.markDailyPromptShown()
                true
            }
            else -> false
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
        items(results, key = { it.pageId }) { result ->
            val onRelevanceClick: (Boolean) -> Unit = { isRelevant ->
                votes[result.pageId] = isRelevant
                // TODO: send the query, search type and chosen card with the vote, once instrumentation is defined.
                onFeedbackSubmit()
            }
            SearchResultCard(
                searchResult = result,
                isAttributionLoading = result.pageId in SearchResultCardPlaygroundSamples.attributionLoadingPageIds,
                relevance = votes[result.pageId],
                onRelevanceClick = onRelevanceClick.takeIf { isFeedbackShown },
                onItemClick = {}
            )
        }
    }
}

private enum class PlaygroundFeedbackMode(val label: String) {
    ALWAYS("Always show"),
    DAILY_LIMIT("Once per day")
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
        SearchResultCardPlaygroundScreen(
            initialTheme = Theme.LIGHT,
            wikiSiteFor = { WikiSite.preview(it) },
            onBackClick = {}
        )
    }
}
