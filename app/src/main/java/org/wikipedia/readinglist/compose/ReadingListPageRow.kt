package org.wikipedia.readinglist.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.ContainingList
import org.wikipedia.readinglist.ReadingListPageUiModel
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@Composable
fun ReadingListPageRow(
    page: ReadingListPageUiModel,
    containingLists: List<ContainingList>,
    modifier: Modifier = Modifier,
    downloadProgress: Int = 0,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: () -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onToggleOfflineClick: () -> Unit = {},
    onChipClick: (Long) -> Unit = {}
) {
    Column(
        modifier = modifier
            .combinedClickable(
                onClick = if (isSelectionMode) onSelectionChange else onClick,
                onLongClick = if (isSelectionMode) onSelectionChange else onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionChange() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ComposeColors.Blue600,
                        uncheckedColor = WikipediaTheme.colors.primaryColor
                    ),
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (page.isAvailable) 1f else 0.5f)
            ) {
                HtmlText(
                    text = page.title,
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!page.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = page.description,
                        color = WikipediaTheme.colors.secondaryColor,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 24.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isSelectionMode && (!page.offline || page.saving)) {
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(32.dp)
                        .clickable(onClick = onToggleOfflineClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (downloadProgress in 1 until 100) {
                        CircularProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.size(28.dp),
                            color = WikipediaTheme.colors.progressiveColor,
                            trackColor = WikipediaTheme.colors.borderColor,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (page.saving) R.drawable.ic_download_in_progress
                                else R.drawable.ic_download_circle_gray_24dp
                            ),
                            contentDescription = stringResource(R.string.reading_list_article_make_offline),
                            tint = if (page.saving) WikipediaTheme.colors.progressiveColor
                            else WikipediaTheme.colors.placeholderColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            ArticleThumbnail(
                thumbUrl = page.thumbUrl,
                modifier = Modifier.alpha(if (page.isAvailable) 1f else 0.5f)
            )
        }

        // Containing-list chips span the full width below the top row.
        if (containingLists.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = spacedBy(8.dp)
            ) {
                containingLists.forEach { list ->
                    ListChip(
                        title = list.title,
                        onClick = {
                            if (isSelectionMode) onSelectionChange() else onChipClick(list.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListChip(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = onClick,
        label = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark_white_24dp),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = WikipediaTheme.colors.paperColor,
            labelColor = WikipediaTheme.colors.secondaryColor,
            iconContentColor = WikipediaTheme.colors.secondaryColor
        ),
        border = BorderStroke(width = 1.dp, color = WikipediaTheme.colors.borderColor),
        modifier = modifier
    )
}

@Composable
private fun ArticleThumbnail(thumbUrl: String?, modifier: Modifier = Modifier) {
    val thumbnail = modifier
        .size(56.dp)
        .clip(RoundedCornerShape(8.dp))
    AsyncImage(
        model = ImageService.getRequest(context = LocalContext.current, url = thumbUrl),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(WikipediaTheme.colors.borderColor),
        error = ColorPainter(WikipediaTheme.colors.borderColor),
        modifier = thumbnail
    )
}

@Preview
@Composable
private fun ReadingListPageRowPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ReadingListPageRow(
            page = ReadingListPageUiModel(
                id = 1,
                title = "Higgs boson",
                description = "Elementary particle in the Standard Model of physics",
                thumbUrl = null,
                lang = "en",
                apiTitle = "Random",
                offline = true,
                saving = true,
                isAvailable = false
            ),
            downloadProgress = 42,
            containingLists = listOf(ContainingList(1, "Physics"), ContainingList(2, "Top read"))
        )
    }
}

@Preview
@Composable
private fun ReadingListPageRowOfflinePreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ReadingListPageRow(
            page = ReadingListPageUiModel(
                id = 2,
                title = "Test2",
                description = "Available offline",
                thumbUrl = null,
                lang = "en",
                apiTitle = "Test2",
                offline = true,
                saving = false,
                isAvailable = true
            ),
            containingLists = listOf(ContainingList(1, "Physics"), ContainingList(2, "Top read"))
        )
    }
}

@Preview
@Composable
private fun ReadingListPageRowSelectedPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ReadingListPageRow(
            page = ReadingListPageUiModel(
                id = 3,
                title = "Selected article",
                description = "Article selected from All articles",
                thumbUrl = null,
                lang = "en",
                apiTitle = "Selected_article",
                offline = false,
                saving = false,
                isAvailable = true
            ),
            containingLists = listOf(ContainingList(1, "Saved pages")),
            isSelectionMode = true,
            isSelected = true
        )
    }
}
