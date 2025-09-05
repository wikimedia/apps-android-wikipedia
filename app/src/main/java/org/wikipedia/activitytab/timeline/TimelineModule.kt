package org.wikipedia.activitytab.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.extensions.isToday
import org.wikipedia.extensions.isYesterday
import org.wikipedia.theme.Theme
import org.wikipedia.util.DateUtil
import org.wikipedia.views.imageservice.ImageService
import java.util.Date

// @TODO: MARK_ACTIVITY_TAB retrieve description and thumbnail for contributions through API
@Composable
fun TimelineModule(
    modifier: Modifier = Modifier,
    timelineItem: TimelineItem,
    onItemClick: (TimelineItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onItemClick(timelineItem) })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (timelineItem.activitySource) {
            ActivitySource.EDIT -> R.drawable.ic_mode_edit_white_24dp
            ActivitySource.SEARCH -> R.drawable.search_bold
            ActivitySource.LINK -> R.drawable.ic_link_black_24dp
            ActivitySource.BOOKMARKED -> R.drawable.ic_bookmark_white_24dp
            null -> null
        }
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = null
            )
        }
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            HtmlText(
                text = timelineItem.displayTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    color = WikipediaTheme.colors.primaryColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            if (!timelineItem.description.isNullOrEmpty()) {
                Text(
                    text = timelineItem.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (timelineItem.activitySource == ActivitySource.EDIT) {
                Button(
                    modifier = modifier.padding(top = 8.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.additionColor,
                        contentColor = WikipediaTheme.colors.secondaryColor,
                    ),
                    onClick = { onItemClick(timelineItem) },
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.outline_difference_24),
                        tint = WikipediaTheme.colors.secondaryColor,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = stringResource(R.string.activity_tab_timeline_view_changes_button)
                    )
                }
            }
        }
        if (!timelineItem.thumbnailUrl.isNullOrEmpty()) {
            val request =
                ImageService.getRequest(LocalContext.current, url = timelineItem.thumbnailUrl)
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

@Composable
fun TimelineModuleEmptyView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            text = stringResource(R.string.activity_tab_timeline_today),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = WikipediaTheme.colors.primaryColor
        )
        Image(
            modifier = Modifier
                .size(164.dp),
            painter = painterResource(R.drawable.illustration_activity_tab_empty),
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(R.string.activity_tab_timeline_empty_state_title),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(R.string.activity_tab_timeline_empty_state_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.primaryColor
        )
    }
}

// Date Separator Composable
@Composable
fun TimelineDateSeparator(
    date: Date,
    modifier: Modifier = Modifier
) {
    val dateText = when {
        date.isToday() -> stringResource(R.string.activity_tab_timeline_today)
        date.isYesterday() -> stringResource(R.string.activity_tab_timeline_yesterday)
        else -> DateUtil.toRelativeDateString(date)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            text = DateUtil.getMMMMdYYYY(date, false),
            style = MaterialTheme.typography.bodySmall,
            color = WikipediaTheme.colors.secondaryColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineItemPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        TimelineModule(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            timelineItem = TimelineItem(
                id = 1,
                pageId = 1,
                displayTitle = "1980s professional wrestling boxing",
                description = "Era of professional wrestling",
                thumbnailUrl = "",
                timestamp = Date(),
                source = 1,
                activitySource = ActivitySource.EDIT
            ),
            onItemClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineDateSeparatorPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        TimelineDateSeparator(
            date = Date()
        )
    }
}

@Preview
@Composable
private fun TimelineModuleEmptyViewPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        TimelineModuleEmptyView(
            modifier = Modifier
                .padding(16.dp)
        )
    }
}
