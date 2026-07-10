package org.wikipedia.readinglist.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.ReadingListPageUiModel
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@Composable
fun ReadingListPageRow(
    page: ReadingListPageUiModel,
    containingLists: List<String>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    onChipClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
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

            // Offline/download action: shown when the article isn't saved offline
            if (!page.offline) {
                Spacer(modifier = Modifier.width(16.dp))
                Image(
                    painter = painterResource(R.drawable.ic_download_circle_gray_24dp),
                    contentDescription = stringResource(R.string.reading_list_article_make_offline),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(24.dp)
                        .clickable(onClick = onActionClick)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
            ArticleThumbnail(page.thumbUrl)
        }

        // Containing-list chips span the full width below the top row.
        if (containingLists.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = spacedBy(8.dp)
            ) {
                containingLists.forEach { title ->
                    ListChip(title = title, onClick = { onChipClick(title) })
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
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = WikipediaTheme.colors.borderColor,
            labelColor = WikipediaTheme.colors.primaryColor
        ),
        border = BorderStroke(width = 1.dp, color = WikipediaTheme.colors.primaryColor.copy(alpha = 0.5f)),
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
                apiTitle = "Higgs_boson",
                offline = false
            ),
            containingLists = listOf("Physics", "Top read")
        )
    }
}
