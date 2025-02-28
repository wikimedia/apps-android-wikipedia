package org.wikipedia.language.langList

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.SearchTopAppBar
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun LanguagesListParentScreen(
    modifier: Modifier = Modifier,
    vieModel: LanguagesViewModel = viewModel(),
    onBackButtonClick: () -> Unit
) {
    val uiState = vieModel.uiState.collectAsState().value
    BaseTheme {
        LanguagesListScreen(
            modifier = modifier,
            languages = uiState.languagesItems,
            isSiteInfoLoaded = uiState.isSiteInfoLoaded,
            onBackButtonClick = onBackButtonClick,
            onSearchQueryChange = { query ->
                vieModel.updateSearchTerm(query)
            }
        )
    }
}

@Composable
fun LanguagesListScreen(
    modifier: Modifier = Modifier,
    languages: List<LanguagesViewModel.LanguageListItem>,
    isSiteInfoLoaded: Boolean = false,
    onBackButtonClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    var isSearchActive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                SearchTopAppBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        onSearchQueryChange(it)
                    },
                    onBackButtonClick = {
                        isSearchActive = false
                        onSearchQueryChange("")
                    }
                )
            } else {
                WikiTopAppBar(
                    title = context.getString(R.string.languages_list_activity_title),
                    onNavigationClick = onBackButtonClick,
                    actions = {
                        IconButton(
                            onClick = {
                                isSearchActive = true
                            },
                            content = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = WikipediaTheme.colors.primaryColor
                                )
                            }
                        )
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSiteInfoLoaded) {
                CircularProgressIndicator(
                    color = WikipediaTheme.colors.progressiveColor
                )
            }
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        if (languages.isEmpty()) {
            SearchEmptyView(
                modifier = Modifier
                    .fillMaxSize(),
                emptyTexTitle = context.getString(R.string.search_no_results_found)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(languages) { languageItem ->
                if (languageItem.isHeader) {
                    ListHeader(
                        modifier = Modifier
                            .padding(
                                start = 16.dp,
                                top = 8.dp,
                                end = 16.dp
                            ),
                        title = languageItem.headerText
                    )
                } else {
                    LanguageListItemView(
                        modifier = Modifier
                            .padding(16.dp),
                        localizedLanguageName = languageItem.localizedName,
                        subtitle = languageItem.canonicalName
                    )
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
    Text(
        modifier = modifier,
        text = title,
        style = titleStyle,
    )
}

@Composable
fun LanguageListItemView(
    modifier: Modifier = Modifier,
    localizedLanguageName: String,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = localizedLanguageName,
            style = WikipediaTheme.typography.h3.copy(
                color = WikipediaTheme.colors.primaryColor,
                textAlign = TextAlign.Center
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

@Composable
fun SearchEmptyView(
    modifier: Modifier = Modifier,
    emptyTexTitle: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(ComposeColors.White)
                .padding(20.dp),
            imageVector = Icons.Outlined.Search,
            tint = ComposeColors.Gray500,
            contentDescription = null
        )
        Text(
            modifier = Modifier
                .padding(top = 24.dp),
            text = emptyTexTitle,
            style = WikipediaTheme.typography.p.copy(
                color = WikipediaTheme.colors.primaryColor
            )
        )
    }
}
