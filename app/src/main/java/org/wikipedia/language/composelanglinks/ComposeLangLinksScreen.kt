package org.wikipedia.language.composelanglinks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBarWithSearch
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun ComposeLangLinksParentScreen(
    modifier: Modifier = Modifier,
    viewModel: ComposeLangLinksViewModel = viewModel(),
    onLanguageSelected: (ComposeLangLinksViewModel.LangLinksItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    ComposeLangLinksScreen(
        isLoading = uiState.isLoading,
        isSiteInfoLoaded = uiState.isSiteInfoLoaded,
        langLinksItem = uiState.langLinksItems,
        onLanguageSelected = onLanguageSelected
    )
}

@Composable
fun ComposeLangLinksScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    isSiteInfoLoaded: Boolean = true,
    langLinksItem: List<ComposeLangLinksViewModel.LangLinksItem>,
    onLanguageSelected: (ComposeLangLinksViewModel.LangLinksItem) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            WikiTopAppBarWithSearch(
                appBarTitle = context.getString(R.string.langlinks_activity_title),
                placeHolderTitle = "",
                searchQuery = searchQuery,
                onSearchQueryChange = {},
                onBackButtonClick = {}
            )
        },
        floatingActionButton = {
            if (isLoading && isSiteInfoLoaded) {
                CircularProgressIndicator(
                    color = WikipediaTheme.colors.progressiveColor
                )
            }
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            items(langLinksItem) { item ->
                if (item.isHeader) {
                    ListHeader(
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp),
                        title = "All languages",
                    )
                } else {
                    LangLinksItemView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = { onLanguageSelected(item) }
                            ),
                        localizedLanguageName = item.localizedName,
                        canonicalName = item.canonicalName,
                        articleName = item.articleName
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
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = localizedLanguageName,
            style = WikipediaTheme.typography.h3.copy(
                color = WikipediaTheme.colors.primaryColor,
            )
        )
        if (!canonicalName.isNullOrEmpty()) {
            Text(
                text = canonicalName,
                style = WikipediaTheme.typography.list.copy(
                    color = WikipediaTheme.colors.secondaryColor,
                    textAlign = TextAlign.Center
                )
            )
        }
        Text(
            text = AnnotatedString.fromHtml(articleName),
            style = WikipediaTheme.typography.list.copy(
                color = WikipediaTheme.colors.secondaryColor,
                textAlign = TextAlign.Center
            )
        )
    }
}
