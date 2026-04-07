package org.wikipedia.feed.topread

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil

@Composable
fun TopReadModule(
    topRead: TopRead,
    onOverflowClick: () -> Unit,
    onItemClick: (PageSummary) -> Unit,
    onItemOverflowClick: (PageSummary) -> Unit,
    onFooterClick: () -> Unit
) {
    val maxTopReadItems = 5
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.view_top_read_card_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.view_top_read_card_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.secondaryColor
                )
            }
            IconButton(
                onClick = onOverflowClick,
                content = {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                        contentDescription = stringResource(R.string.search_clear_query_content_description),
                        tint = WikipediaTheme.colors.placeholderColor
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            topRead.articles.take(maxTopReadItems).forEachIndexed { index, article ->
                TopReadItem(
                    context = context,
                    rank = index + 1,
                    pageSummary = article,
                    onClick = onItemClick,
                    onMoreClick = onItemOverflowClick
                )
            }
        }

        TextButton(
            modifier = Modifier
                .align(Alignment.End),
            onClick = onFooterClick
        ) {
            Text(
                text = stringResource(R.string.view_top_read_card_action),
                style = MaterialTheme.typography.labelLarge,
                color = WikipediaTheme.colors.progressiveColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
                contentDescription = stringResource(R.string.view_top_read_card_action),
                tint = WikipediaTheme.colors.progressiveColor
            )
        }
    }
}

@Composable
fun TopReadItem(
    context: Context,
    rank: Int,
    pageSummary: PageSummary,
    onClick: (PageSummary) -> Unit,
    onMoreClick: (PageSummary) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = {
                onClick(pageSummary)
            })
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WikipediaTheme.colors.borderColor)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = WikipediaTheme.colors.progressiveColor,
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    fontSize = 16.sp,
                    color = WikipediaTheme.colors.paperColor,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                HtmlText(
                    text = pageSummary.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(
                        text = StringUtil.getPageViewText(context, pageSummary.views),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.view_top_read_card_pageviews_views_suffix),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }
            }

            pageSummary.thumbnailUrl?.let {
                AsyncImage(
                    model = pageSummary.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            IconButton(
                onClick = {
                    onMoreClick(pageSummary)
                },
                content = {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                        contentDescription = stringResource(R.string.search_clear_query_content_description),
                        tint = WikipediaTheme.colors.placeholderColor
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopReadCardPreview() {
    val article = PageSummary(
        displayTitle = "Test Article",
        prefixTitle = "Test_Article",
        description = "This is a test article.",
        extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        thumbnail = null,
        lang = "en"
    )
    BaseTheme(currentTheme = Theme.LIGHT) {
        TopReadModule(
            topRead = TopRead(
                articles = listOf(article, article, article, article, article)
            ),
            onFooterClick = {},
            onOverflowClick = {},
            onItemClick = {},
            onItemOverflowClick = {}
        )
    }
}
