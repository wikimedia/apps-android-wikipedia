package org.wikipedia.activitytab.timeline

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.views.imageservice.ImageService
import java.time.format.DateTimeFormatter
import java.util.Date

@Composable
fun Timeline(
    modifier: Modifier = Modifier,
    timelineItem: TimelineItem,
    onItemClick: (TimelineItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onItemClick(timelineItem) })
            .padding(16.dp),
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
                .weight(1f)
        ) {
            Text(
                text = timelineItem.apiTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            timelineItem.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (timelineItem.activitySource == ActivitySource.EDIT) {
                Button(
                    modifier = modifier.padding(top = 8.dp, bottom = 16.dp),
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
                        text = "View changes"
                    )
                }
            }
        }
        if (timelineItem.thumbnailUrl != null) {
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
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }
}

// Date Separator Composable
@Composable
fun TimelineDateSeparator(
    date: Date,
    modifier: Modifier = Modifier
) {
    val localDate = date.toLocalDate()
    val dateText = when {
        date.isToday() -> "Today"
        date.isYesterday() -> "Yesterday"
        else -> formatDate(localDate)
    }

    val fullDateText = DateTimeFormatter.ofPattern("MMMM d, yyyy").format(localDate)

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
            text = fullDateText,
            style = MaterialTheme.typography.bodySmall,
            color = WikipediaTheme.colors.secondaryColor
        )
    }
}
