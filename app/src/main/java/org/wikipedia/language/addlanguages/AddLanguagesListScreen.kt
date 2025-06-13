package org.wikipedia.language.addlanguages

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.compose.components.SearchEmptyView
import org.wikipedia.compose.components.WikiTopAppBarWithSearch
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UiState

@Composable
fun LanguagesListScreen(
    modifier: Modifier = Modifier,
    uiState: UiState<List<LanguageListItem>>,
    onBackButtonClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onListItemClick: (code: String) -> Unit,
    onLanguageSearched: (Boolean) -> Unit,
    wikiErrorClickEvents: WikiErrorClickEvents? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val imeHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        // Handle IME (keyboard) insets
        with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }
    } else 0.dp

    Scaffold(
        modifier = modifier,
        topBar = {
            WikiTopAppBarWithSearch(
                appBarTitle = stringResource(R.string.languages_list_activity_title),
                placeHolderTitle = stringResource(R.string.search_hint_search_languages),
                searchQuery = searchQuery,
                onBackButtonClick = onBackButtonClick,
                onSearchQueryChange = { value ->
                    onLanguageSearched(true)
                    searchQuery = value
                    onSearchQueryChange(value)
                }
            )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        when (uiState) {
            UiState.Loading -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp),
                        color = WikipediaTheme.colors.progressiveColor
                    )
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        // Add bottom padding when keyboard is visible for android 15 and above
                        .padding(bottom = imeHeight),
                    contentAlignment = Alignment.Center
                ) {
                    WikiErrorView(
                        modifier = Modifier
                            .fillMaxWidth(),
                        caught = uiState.error,
                        errorClickEvents = wikiErrorClickEvents
                    )
                }
            }
            is UiState.Success -> {
                val languagesItems = uiState.data
                if (languagesItems.isEmpty()) {
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            // Add bottom padding when keyboard is visible for android 15 and above
                            .padding(bottom = imeHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        SearchEmptyView(
                            modifier = Modifier
                                .fillMaxWidth(),
                            emptyTexTitle = stringResource(R.string.langlinks_no_match)
                        )
                    }
                    return@Scaffold
                }

                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .testTag("language_list"),
                ) {
                    itemsIndexed(languagesItems) { index, languageItem ->
                        if (languageItem.headerText.isNotEmpty()) {
                            ListHeader(
                                modifier = Modifier
                                    .height(56.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 4.dp),
                                title = languageItem.headerText
                            )
                        } else {
                            val localizedLanguageName = StringUtil.capitalize(languageItem.localizedName).orEmpty()
                            LanguageListItemView(
                                modifier = Modifier
                                    .clickable(onClick = {
                                        BreadCrumbLogEvent.logClick(context, "listItem.$index")
                                        onListItemClick(languageItem.code)
                                    })
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .testTag(languageItem.canonicalName),
                                localizedLanguageName = localizedLanguageName,
                                subtitle = languageItem.canonicalName
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListHeader(
    title: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall.copy(
        color = WikipediaTheme.colors.primaryColor,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp
    )
) {
    Box(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart),
            text = title,
            style = titleStyle,
        )
    }
}

@Composable
fun LanguageListItemView(
    modifier: Modifier = Modifier,
    localizedLanguageName: String,
    subtitle: String? = null
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = localizedLanguageName,
            style = MaterialTheme.typography.titleMedium.copy(
                color = WikipediaTheme.colors.primaryColor,
                fontWeight = FontWeight.Bold,
            )
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = WikipediaTheme.colors.secondaryColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                )
            )
        }
    }
}

@Preview
@Composable
private fun LanguagesListScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        LanguagesListScreen(
            modifier = Modifier
                .fillMaxSize(),
            uiState = UiState.Success(data = listOf(
                LanguageListItem(code = "", headerText = "Languages"),
                LanguageListItem(code = "en", canonicalName = "English", localizedName = "English"),
                LanguageListItem(code = "he", canonicalName = "Hebrew", localizedName = "עברית")
            )
            ),
            { },
            { },
            { },
            { }
        )
    }
}
