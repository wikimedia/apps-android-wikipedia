package org.wikipedia.search.semantic

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.theme.Theme
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.UiState
import org.wikipedia.views.imageservice.ImageService

@Composable
fun SemanticSearchResultsScreen(
    modifier: Modifier = Modifier,
    viewModel: SemanticSearchResultsViewModel,
    onItemClick: (SearchResult, PageTitle, Boolean, Boolean, Int, Location?) -> Unit,
    onCloseClick: () -> Unit,
    onRatingClick: (Boolean, SearchResult) -> Unit,
    onLoading: (Boolean) -> Unit,
) {

    val searchResultsState = viewModel.semanticSearchResultState.collectAsState().value

    val languageCode = viewModel.languageCode
    val layoutDirection =
        if (L10nUtil.isLangRTL(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = modifier
        ) {
            when (searchResultsState) {
                is UiState.Loading -> {
                    onLoading(true)
                    // TODO: show skeleton loader
                }

                is UiState.Success -> {
                    onLoading(false)
                    val results = searchResultsState.data
                    if (results.isEmpty()) {
                        // TODO: show empty message
                        return@CompositionLocalProvider
                    }
                    SemanticSearchResultsContent(
                        viewModel = viewModel,
                        items = results,
                        onCloseClick = onCloseClick
                    )
                }

                is UiState.Error -> {
                    onLoading(false)
                }
            }
        }
    }
}

@Composable
fun SemanticSearchResultsContent(
    modifier: Modifier = Modifier,
    viewModel: SemanticSearchResultsViewModel,
    items: List<SearchResult>,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.backgroundColor)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = WikipediaTheme.colors.borderColor,
                        shape = RoundedCornerShape(size = 16.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    modifier = Modifier,
                    painter = painterResource(R.drawable.ic_experiment_24dp),
                    tint = WikipediaTheme.colors.secondaryColor,
                    contentDescription = null
                )

                Text(
                    text = stringResource(R.string.donation_reminders_beta_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.primaryColor
                )
            }

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_black_24dp),
                    contentDescription = stringResource(R.string.semantic_search_results_close_button_content_description),
                    tint = WikipediaTheme.colors.primaryColor
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(items.size, key = { items[it].pageTitle }) { index ->
                SemanticSearchResultCard(
                    viewModel = viewModel,
                    searchResult = items[index]
                )
            }
        }
    }
}

@Composable
fun SemanticSearchResultCard(
    viewModel: SemanticSearchResultsViewModel,
    searchResult: SearchResult,
    modifier: Modifier = Modifier
) {
    val articlePath = listOfNotNull(
        searchResult.pageTitle.displayText.takeIf { it.isNotBlank() },
        searchResult.sectionTitle?.takeIf { it.isNotBlank() }
    ).joinToString(" | ")

    val editCount = searchResult.editCounts ?: 0
    val referenceCounts = searchResult.referenceCounts ?: 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.backgroundColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = viewModel.quotationMarkMap[viewModel.languageCode] ?: "",
                    fontSize = 22.sp,
                    lineHeight = 20.sp,
                    color = WikipediaTheme.colors.primaryColor
                )
                Spacer(modifier = Modifier.width(6.dp))

                HtmlText(
                    text = searchResult.snippet.orEmpty(),
                    color = WikipediaTheme.colors.primaryColor,
                    linkStyle = TextLinkStyles(
                        style = SpanStyle(
                            color = WikipediaTheme.colors.progressiveColor,
                            fontSize = 16.sp
                        )
                    ),
                    linkInteractionListener = {
                        val url = (it as LinkAnnotation.Url).url
                        // TODO: handle link click
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!searchResult.pageTitle.thumbUrl.isNullOrEmpty()) {
                    val request =
                        ImageService.getRequest(
                            LocalContext.current,
                            url = searchResult.pageTitle.thumbUrl
                        )
                    AsyncImage(
                        model = request,
                        placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                        error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = articlePath,
                    fontSize = 12.sp,
                    color = WikipediaTheme.colors.primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_group_24dp),
                        modifier = Modifier.size(15.dp),
                        contentDescription = null,
                        tint = WikipediaTheme.colors.secondaryColor,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pluralStringResource(R.plurals.semantic_search_result_contributors, editCount, editCount),
                        fontSize = 12.sp,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_references_24dp),
                        modifier = Modifier.size(15.dp),
                        contentDescription = null,
                        tint = WikipediaTheme.colors.secondaryColor,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pluralStringResource(R.plurals.semantic_search_result_references, referenceCounts, referenceCounts),
                        fontSize = 12.sp,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SemanticSearchResultsScreenPreview() {
    val wikiSite = WikiSite.preview()
    val pageTitle = PageTitle("Beyoncé", wikiSite).apply {
        description = "American singer, songwriter, and actress"
        thumbUrl = "https://example"
    }
    val snippet = "Beyoncé Giselle Knowles-Carter is an <a href='#'>American singer</a>, songwriter, actress, and businesswoman. Born and raised in Houston, Texas, she performed in various singing and dancing competitions as a child. She rose to fame in the late 1990s as the lead singer of Destiny's Child, one of the world's best"

    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SemanticSearchResultsContent(
            onCloseClick = {},
            viewModel = viewModel(),
            items = listOf(
                SearchResult(
                    pageTitle = pageTitle,
                    searchResultType = SearchResult.SearchResultType.SEMANTIC,
                    snippet = snippet
                )
            )
        )
    }
}
