package org.wikipedia.wiktionary

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.restbase.RbDefinition
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

@Composable
fun WiktionaryDialogScreen(
    viewModel: WiktionaryViewModel,
    onDialogLinkClick: (url: String) -> Unit
) {
    val uiState = viewModel.uiState.collectAsState().value
    WiktionaryDialogContent(
        title = StringUtil.removeUnderscores(StringUtil.removeSectionAnchor(viewModel.selectedText)),
        showNoDefinitions = uiState is Resource.Error,
        showProgress = uiState is Resource.Loading
    ) {
        if (uiState is Resource.Success) {
            Column {
                uiState.data.forEach {
                    DefinitionList(it, onDialogLinkClick)
                }
            }
        }
    }
}

@Composable
fun WiktionaryDialogContent(
    title: String,
    showNoDefinitions: Boolean = false,
    showProgress: Boolean = false,
    definitionsContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .heightIn(min = dimensionResource(R.dimen.bottomSheetPeekHeight))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_define),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 12.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = title,
                color = WikipediaTheme.colors.primaryColor,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            )
        }

        HorizontalDivider(
            color = WikipediaTheme.colors.borderColor,
            thickness = 0.5.dp,
            modifier = Modifier.fillMaxWidth()
        )

        if (showNoDefinitions) {
            Text(
                text = stringResource(R.string.wiktionary_no_definitions_found),
                color = WikipediaTheme.colors.primaryColor,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            definitionsContent()
        }
    }

    if (showProgress) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 128.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = WikipediaTheme.colors.progressiveColor,
            )
        }
    }
}

@Composable
fun DefinitionList(
    usage: RbDefinition.Usage,
    onDialogLinkClick: (url: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = usage.partOfSpeech,
            fontSize = 14.sp,
            color = WikipediaTheme.colors.placeholderColor,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        var index = 0
        usage.definitions.forEach {
            if (it.definition.isNotEmpty()) {
                DefinitionWithExamples(
                    definition = it,
                    count = ++index,
                    onDialogLinkClick = onDialogLinkClick
                )
            }
        }
    }
}

@Composable
fun DefinitionWithExamples(
    definition: RbDefinition.Definition,
    count: Int,
    onDialogLinkClick: (url: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        SelectionContainer {
            HtmlText(
                text = "$count. ${definition.definition}",
                modifier = Modifier.padding(vertical = 4.dp),
                style = TextStyle(
                    color = WikipediaTheme.colors.primaryColor,
                    fontSize = 14.sp,
                ),
                linkInteractionListener = {
                    val url = (it as LinkAnnotation.Url).url
                    onDialogLinkClick(url)
                }
            )
        }

        definition.examples?.forEach { example ->
            SelectionContainer {
                HtmlText(
                    text = example,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    style = TextStyle(
                        color = WikipediaTheme.colors.primaryColor,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic
                    ),
                    linkInteractionListener = {
                        val url = (it as LinkAnnotation.Url).url
                        onDialogLinkClick(url)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun WiktionaryDialogPreview() {
    WiktionaryDialogContent(
        title = "Lorem ipsum",
        showNoDefinitions = false,
        showProgress = false
    ) {
        Text(stringResource(R.string.wiktionary_no_definitions_found))
    }
}
