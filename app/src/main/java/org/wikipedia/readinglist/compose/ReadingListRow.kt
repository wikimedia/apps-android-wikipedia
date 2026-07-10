package org.wikipedia.readinglist.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.ReadingListUiModel
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@Composable
fun ReadingListRow(
    list: ReadingListUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = list.title,
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (list.isNew) {
                    Text(
                        text = stringResource(R.string.shareable_reading_lists_new_indicator),
                        color = WikipediaTheme.colors.successColor,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = statisticalSummary(list),
                color = WikipediaTheme.colors.secondaryColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 24.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val description = if (list.isDefault) {
                stringResource(R.string.default_reading_list_description)
            } else {
                list.description
            }
            if (!description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = description,
                    color = WikipediaTheme.colors.secondaryColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        ReadingListThumbnails(list)
    }
}

@Composable
private fun statisticalSummary(list: ReadingListUiModel): String {
    val bytesPerUnit = integerResource(R.integer.reading_list_item_size_bytes_per_unit)
    val totalListSize = list.sizeBytesFromPages / maxOf(1, bytesPerUnit).toFloat()
    return if (totalListSize > 0f) {
        pluralStringResource(R.plurals.format_reading_list_statistical_summary, list.totalPages, list.totalPages, totalListSize)
    } else {
        pluralStringResource(R.plurals.format_reading_list_statistical_summary_without_size, list.totalPages, list.totalPages)
    }
}

@Composable
private fun ReadingListThumbnails(list: ReadingListUiModel, modifier: Modifier = Modifier) {
    val gridSize = 72.dp
    val showEmptyBookmark = list.isDefault && list.totalPages == 0

    if (showEmptyBookmark) {
        Icon(
            painter = painterResource(R.drawable.ic_bookmark_gray_24dp),
            contentDescription = null,
            tint = WikipediaTheme.colors.placeholderColor,
            modifier = modifier
                .size(gridSize)
                .clip(RoundedCornerShape(8.dp))
                .background(WikipediaTheme.colors.borderColor)
                .padding(16.dp)
        )
        return
    }

    Column(
        modifier = modifier
            .size(gridSize)
            .clip(RoundedCornerShape(16.dp)),
        verticalArrangement = spacedBy(1.dp)
    ) {
        Row(modifier = Modifier.weight(1f)) {
            ThumbnailCell(list.thumbUrls.getOrNull(0), Modifier.weight(1f).fillMaxHeight())
            Spacer(modifier.width(1.dp))
            ThumbnailCell(list.thumbUrls.getOrNull(1), Modifier.weight(1f).fillMaxHeight())
        }
        Row(modifier = Modifier.weight(1f)) {
            ThumbnailCell(list.thumbUrls.getOrNull(2), Modifier.weight(1f).fillMaxHeight())
            Spacer(modifier.width(1.dp))
            ThumbnailCell(list.thumbUrls.getOrNull(3), Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun ThumbnailCell(url: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageService.getRequest(context = LocalContext.current, url = url),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(WikipediaTheme.colors.borderColor),
        error = ColorPainter(WikipediaTheme.colors.borderColor),
        modifier = modifier
    )
}

@Preview
@Composable
private fun ReadingListRowPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ReadingListRow(
            list = ReadingListUiModel(
                id = 1,
                title = "Physics",
                description = "Articles about classical and quantum mechanics",
                isDefault = false,
                totalPages = 12,
                sizeBytesFromPages = 8_500_000,
                thumbUrls = emptyList(),
                isNew = true
            )
        )
    }
}

@Preview
@Composable
private fun ReadingListRowDefaultEmptyPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ReadingListRow(
            list = ReadingListUiModel(
                id = 2,
                title = "Saved",
                description = null,
                isDefault = true,
                totalPages = 0,
                sizeBytesFromPages = 0
            )
        )
    }
}
