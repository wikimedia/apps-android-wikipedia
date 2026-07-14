package org.wikipedia.readinglist.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.ReadingListUiModel
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@Composable
fun ReadingListRow(
    list: ReadingListUiModel,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: () -> Unit = {},
    onClick: () -> Unit = {},
    onMenuAction: (ReadingListMenuAction) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true })
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectionChange() },
                colors = CheckboxDefaults.colors(
                    checkedColor = ComposeColors.Blue600,
                    uncheckedColor = WikipediaTheme.colors.primaryColor,
                ),
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(end = 16.dp)
            )
        }
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

        Box {
            ReadingListItemMenu(
                expanded = menuExpanded,
                isDefault = list.isDefault,
                isSelected = isSelected,
                onDismiss = { menuExpanded = false },
                onAction = {
                    menuExpanded = false
                    onMenuAction(it)
                }
            )
        }
    }
}

enum class ReadingListMenuAction {
    Rename, Delete, SaveAllOffline, RemoveAllOffline, Export, Select, Share
}

@Composable
private fun ReadingListItemMenu(
    expanded: Boolean,
    isDefault: Boolean,
    isSelected: Boolean,
    onDismiss: () -> Unit,
    onAction: (ReadingListMenuAction) -> Unit
) {
    // Default list will not have rename and delete option
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = WikipediaTheme.colors.paperColor
    ) {
        if (!isDefault) {
            ReadingListMenuItem(
                text = stringResource(R.string.reading_list_menu_rename),
                onClick = { onAction(ReadingListMenuAction.Rename) }
            )
        }
        ReadingListMenuItem(
            text = stringResource(R.string.reading_list_action_menu_save_all_for_offline),
            onClick = { onAction(ReadingListMenuAction.SaveAllOffline) }
        )
        ReadingListMenuItem(
            text = stringResource(R.string.reading_list_action_menu_remove_all_from_offline),
            onClick = { onAction(ReadingListMenuAction.RemoveAllOffline) }
        )
        ReadingListMenuItem(
            text = stringResource(R.string.reading_list_menu_export),
            onClick = { onAction(ReadingListMenuAction.Export) }
        )
        ReadingListMenuItem(
            text = stringResource(if (isSelected) R.string.reading_list_menu_unselect else R.string.reading_list_menu_select),
            onClick = { onAction(ReadingListMenuAction.Select) }
        )
        if (!isDefault) {
            ReadingListMenuItem(
                text = stringResource(R.string.reading_list_menu_delete),
                onClick = { onAction(ReadingListMenuAction.Delete) }
            )
        }
        ReadingListMenuItem(
            text = stringResource(R.string.reading_list_share_menu_label),
            onClick = { onAction(ReadingListMenuAction.Share) }
        )
    }
}

@Composable
private fun ReadingListMenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )
        },
        onClick = onClick
    )
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
