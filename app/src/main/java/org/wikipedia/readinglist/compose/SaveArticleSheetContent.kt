package org.wikipedia.readinglist.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.SaveArticleUiModel
import org.wikipedia.readinglist.SaveCollectionUiModel
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveArticleSheetContent(
    article: SaveArticleUiModel,
    collections: List<SaveCollectionUiModel>,
    modifier: Modifier = Modifier,
    onArticleHeaderClick: () -> Unit = {},
    onCreateCollectionClick: () -> Unit = {},
    onCollectionRowClick: (Long) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        BottomSheetDefaults.DragHandle(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = WikipediaTheme.colors.inactiveColor
        )
        ArticleHeader(
            article = article,
            onClick = onArticleHeaderClick
        )
        HorizontalDivider(
            color = WikipediaTheme.colors.borderColor,
            thickness = 0.5.dp
        )
        Column(
            modifier = Modifier
                .background(WikipediaTheme.colors.backgroundColor)
        ) {
            if (collections.isEmpty()) {
                EmptyCollections(onStartCollectionClick = onCreateCollectionClick)
            } else {
                CollectionsHeader(onCreateClick = onCreateCollectionClick)
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(
                        items = collections,
                        key = { it.id }
                    ) { collection ->
                        CollectionRow(
                            collection = collection,
                            onClick = { onCollectionRowClick(collection.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleHeader(
    article: SaveArticleUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            HtmlText(
                text = article.title,
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.25.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!article.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.description,
                    color = WikipediaTheme.colors.secondaryColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.25.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Thumbnail(thumbUrl = article.thumbUrl)

        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (article.isSaved) R.drawable.ic_bookmark_white_24dp
                    else R.drawable.ic_bookmark_border_white_24dp
                ),
                contentDescription = null,
                tint = WikipediaTheme.colors.primaryColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EmptyCollections(
    onStartCollectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.reading_lists_save_sheet_empty_title),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.reading_lists_save_sheet_empty_message),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(
            onClick = onStartCollectionClick,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.reading_lists_save_sheet_empty_action),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun CollectionsHeader(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.reading_lists_tab_collections),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.15.sp
            ),
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onCreateClick,
            content = {
                Icon(
                    painter = painterResource(R.drawable.ic_add_gray_white_24dp),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.progressiveColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reading_lists_save_sheet_create),
                    color = WikipediaTheme.colors.progressiveColor,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        )
    }
}

@Composable
private fun CollectionRow(
    collection: SaveCollectionUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = collection.title,
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.format_reading_list_statistical_summary_without_size,
                    collection.totalPages,
                    collection.totalPages
                ),
                color = WikipediaTheme.colors.secondaryColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Thumbnail(thumbUrl = collection.thumbUrl)

        // Not separately clickable: the whole row is the toggle, so the icon only reports state.
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            val iconRes = if (collection.containsArticle) {
                R.drawable.ic_check_circle_black_24dp
            } else {
                R.drawable.ic_add_circle_24dp
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = WikipediaTheme.colors.primaryColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun Thumbnail(
    thumbUrl: String?,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageService.getRequest(context = LocalContext.current, url = thumbUrl),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(WikipediaTheme.colors.borderColor),
        error = ColorPainter(WikipediaTheme.colors.borderColor),
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}

private val previewArticle = SaveArticleUiModel(
    title = "Ethan Hawke",
    description = "American actor",
    thumbUrl = null,
    isSaved = true
)

@Preview
@Composable
private fun SaveArticleSheetEmptyPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        SaveArticleSheetContent(
            // Stands in for the background the bottom sheet dialog provides at runtime.
            modifier = Modifier.background(WikipediaTheme.colors.paperColor),
            article = previewArticle,
            collections = emptyList()
        )
    }
}

@Preview
@Composable
private fun SaveArticleSheetWithCollectionsPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        SaveArticleSheetContent(
            // Stands in for the background the bottom sheet dialog provides at runtime.
            modifier = Modifier.background(WikipediaTheme.colors.paperColor),
            article = previewArticle,
            collections = listOf(
                SaveCollectionUiModel(id = 1, title = "actors", totalPages = 2, containsArticle = true),
                SaveCollectionUiModel(id = 2, title = "fashion designers", totalPages = 17),
                SaveCollectionUiModel(id = 3, title = "random", totalPages = 4, containsArticle = true)
            )
        )
    }
}

@Preview(heightDp = 700)
@Composable
private fun SaveArticleSheetManyCollectionsPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        SaveArticleSheetContent(
            // Stands in for the background the bottom sheet dialog provides at runtime.
            modifier = Modifier.background(WikipediaTheme.colors.paperColor),
            article = previewArticle,
            collections = List(100) {
                SaveCollectionUiModel(
                    id = it.toLong(),
                    title = "Collection ${it + 1}",
                    totalPages = it
                )
            }
        )
    }
}

@Preview
@Composable
private fun SaveArticleSheetWithCollectionsDarkPreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        SaveArticleSheetContent(
            // Stands in for the background the bottom sheet dialog provides at runtime.
            modifier = Modifier.background(WikipediaTheme.colors.paperColor),
            article = previewArticle.copy(isSaved = false),
            collections = listOf(
                SaveCollectionUiModel(id = 1, title = "actors", totalPages = 2),
                SaveCollectionUiModel(id = 2, title = "fashion designers", totalPages = 17)
            )
        )
    }
}
