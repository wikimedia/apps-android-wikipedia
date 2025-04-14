package org.wikipedia.language.composelanglinks

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.SearchEmptyView
import org.wikipedia.compose.components.WikiTopAppBarWithSearch
import org.wikipedia.compose.components.error.ComposeWikiErrorParentView
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.util.UiState

@Composable
fun ComposeLangLinksScreen(
    modifier: Modifier = Modifier,
    uiState: UiState<List<LangLinksItem>>,
    onLanguageSelected: (LangLinksItem) -> Unit,
    wikiErrorClickEvents: WikiErrorClickEvents? = null,
    onBackButtonClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
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
        topBar = {
            WikiTopAppBarWithSearch(
                appBarTitle = context.getString(R.string.langlinks_activity_title),
                placeHolderTitle = context.getString(R.string.langlinks_filter_hint),
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                    onSearchQueryChange(it)
                },
                onBackButtonClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        when (uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        // Add bottom padding when keyboard is visible
                        .padding(bottom = if (isKeyboardVisible) imeHeight else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = WikipediaTheme.colors.progressiveColor
                    )
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        // Add bottom padding when keyboard is visible
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
                val langLinksItem = uiState.data
                if (langLinksItem.isEmpty()) {
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            // Add bottom padding when keyboard is visible
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
                        .padding(paddingValues)
                ) {
                    items(langLinksItem) { item ->
                        if (item.headerText.isNotEmpty()) {
                            ListHeader(
                                modifier = Modifier
                                    .height(56.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 4.dp),
                                title = item.headerText,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true),
                                        onClick = { onLanguageSelected(item) }
                                    )
                            ) {
                                LangLinksItemView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    localizedLanguageName = item.localizedName,
                                    canonicalName = item.canonicalName,
                                    articleName = item.articleName
                                )
                            }
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
fun LangLinksItemView(
    modifier: Modifier = Modifier,
    localizedLanguageName: String,
    canonicalName: String? = null,
    articleName: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = localizedLanguageName,
            style = WikipediaTheme.typography.h3.copy(
                color = WikipediaTheme.colors.primaryColor,
            )
        )
        if (!canonicalName.isNullOrEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = canonicalName,
                style = WikipediaTheme.typography.list.copy(
                    color = WikipediaTheme.colors.secondaryColor
                )
            )
        }
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = articleName,
            style = WikipediaTheme.typography.list.copy(
                color = WikipediaTheme.colors.secondaryColor
            )
        )
    }
}
