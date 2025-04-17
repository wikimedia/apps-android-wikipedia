package org.wikipedia.language.addlanguages

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.compose.components.SearchEmptyView
import org.wikipedia.compose.components.WikiTopAppBarWithSearch
import org.wikipedia.compose.components.error.ComposeWikiErrorParentView
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.WikipediaTheme
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
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val (imeHeight, isKeyboardVisible) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        // Handle IME (keyboard) insets
        val windowInsets = WindowInsets.ime
        val height = with(LocalDensity.current) { windowInsets.getBottom(this).toDp() }
        Pair(height, height > 0.dp)
    } else Pair(0.dp, false)

    Scaffold(
        modifier = modifier,
        topBar = {
            WikiTopAppBarWithSearch(
                appBarTitle = context.getString(R.string.languages_list_activity_title),
                placeHolderTitle = context.getString(R.string.search_hint_search_languages),
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
                        .padding(bottom = if (isKeyboardVisible) imeHeight else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ComposeWikiErrorParentView(
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
                            .padding(bottom = if (isKeyboardVisible) imeHeight else 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SearchEmptyView(
                            modifier = Modifier
                                .fillMaxWidth(),
                            emptyTexTitle = context.getString(R.string.langlinks_no_match)
                        )
                    }
                    return@Scaffold
                }

                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
                            val localizedLanguageName = StringUtil.capitalize(WikipediaApp.instance.languageState.getAppLanguageLocalizedName(languageItem.code).orEmpty()) ?: ""
                            LanguageListItemView(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true),
                                        onClick = {
                                            BreadCrumbLogEvent.logClick(context, "listItem.$index")
                                            onListItemClick(languageItem.code)
                                        }
                                    )
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
    titleStyle: TextStyle = WikipediaTheme.typography.h4.copy(
        color = WikipediaTheme.colors.primaryColor,
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
            style = WikipediaTheme.typography.h3.copy(
                color = WikipediaTheme.colors.primaryColor,
            )
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = WikipediaTheme.typography.list.copy(
                    color = WikipediaTheme.colors.secondaryColor,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
