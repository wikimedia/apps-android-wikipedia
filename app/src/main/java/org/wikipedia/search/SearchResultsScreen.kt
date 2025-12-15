package org.wikipedia.search

import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.extensions.toAnnotatedString
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.views.imageservice.ImageService

@Composable
fun SearchResultsScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchResultsViewModel = viewModel(),
    onNavigateToTitle: (PageTitle, Boolean, Int, Location?) -> Unit
) {
    val searchResults = viewModel.searchResultsFlow.collectAsLazyPagingItems()
    val searchTerm = viewModel.searchTerm.collectAsState()
    Box {
        SearchResultsList(
            modifier = modifier,
            searchResults = searchResults,
            searchTerm = searchTerm.value,
            onItemClick = onNavigateToTitle,
            onItemLongClick = { result, position -> }
        )
    }
}

@Composable
fun SearchResultsList(
    searchResults: LazyPagingItems<SearchResult>,
    searchTerm: String?,
    onItemClick: (PageTitle, Boolean, Int, Location?) -> Unit,
    onItemLongClick: (SearchResult, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn {
        items(
            count = searchResults.itemCount
        ) { index ->
            searchResults[index]?.let { result ->
                SearchResultItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    searchResult = result,
                    searchTerm = searchTerm,
                    onClick = {
                        onItemClick(result.pageTitle, false, index, result.location)
                    },
                    onLongClick = {
                        onItemLongClick(result, index)
                    }
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    searchResult: SearchResult,
    searchTerm: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (SearchResult) -> Unit = {}
) {
    val (pageTitle, redirectFrom, type) = searchResult

    val isRedirect = !redirectFrom.isNullOrEmpty()

    val iconResId = when (type) {
        SearchResult.SearchResultType.HISTORY -> R.drawable.ic_history_24
        SearchResult.SearchResultType.TAB_LIST -> R.drawable.ic_tab_one_24px
        else -> R.drawable.ic_bookmark_border_white_24dp
    }

    val showImage = !pageTitle.thumbUrl.isNullOrEmpty() && type != SearchResult.SearchResultType.SEARCH

    val boldenTitle = remember(pageTitle.displayText, searchTerm) {
        boldenAnnotatedString(pageTitle.displayText, searchTerm)
    }
    Row(
        modifier = modifier
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            // Title with bold style
            Text(
                text = boldenTitle,
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.bodyLarge
            )

            if (isRedirect) {
                Row(
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Image(
                        modifier = Modifier
                            .size(16.dp),
                        painter = painterResource(R.drawable.ic_subdirectory_arrow_right_black_24dp),
                        colorFilter = ColorFilter.tint(color = WikipediaTheme.colors.placeholderColor),
                        contentDescription = null
                    )
                    Text(
                        text = "Redirect from...",
                        color = WikipediaTheme.colors.secondaryColor,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 14.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                val description = pageTitle.description
                if (!description.isNullOrEmpty()) {
                    Text(
                        modifier = Modifier
                            .padding(top = 2.dp),
                        text = description,
                        color = WikipediaTheme.colors.secondaryColor,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 14.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (type != SearchResult.SearchResultType.SEARCH) {
                Image(
                    modifier = Modifier
                        .size(20.dp),
                    painter = painterResource(iconResId),
                    colorFilter = ColorFilter.tint(color = WikipediaTheme.colors.placeholderColor),
                    contentDescription = null
                )
            }

            if (showImage) {
                val request =
                    ImageService.getRequest(
                        LocalContext.current,
                        url = pageTitle.thumbUrl
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
        }
    }
}

fun boldenAnnotatedString(
    text: String,
    query: String?
): AnnotatedString {
    val spanned = StringUtil.fromHtml(text)
    val annotated = spanned.toAnnotatedString()
    if (query.isNullOrEmpty()) {
        return annotated
    }

    val startIndex = annotated.text.indexOf(query, ignoreCase = true)
    if (startIndex >= 0) {
        val builder = AnnotatedString.Builder(annotated)
        builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), startIndex, startIndex + query.length)
        return builder.toAnnotatedString()
    }
    return annotated
}
