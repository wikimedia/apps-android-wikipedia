package org.wikipedia.language.langList

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun LanguagesListParentScreen(
    modifier: Modifier = Modifier,
    vieModel: LanguagesViewModel = viewModel()
) {
    val uiState = vieModel.uiState.collectAsState().value
    BaseTheme {
        LanguagesListScreen(
            modifier = Modifier,
            languages = uiState.languagesItems,
            isSiteInfoLoaded = uiState.isSiteInfoLoaded
        )
    }
}


@Composable
fun LanguagesListScreen(
    modifier: Modifier = Modifier,
    languages: List<LanguagesViewModel.LanguageListItem>,
    isSiteInfoLoaded: Boolean = false

) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = context.getString(R.string.languages_list_activity_title),
                onNavigationClick = {},
                actions = {
                    IconButton(
                        onClick = {},
                        content = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
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
        LazyColumn(
            modifier = Modifier
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


@Preview
@Composable
private fun LanguagesListScreenPreview() {
    BaseTheme {
        LanguagesListScreen(
            modifier = Modifier,
            languages = listOf()
        )
    }
}