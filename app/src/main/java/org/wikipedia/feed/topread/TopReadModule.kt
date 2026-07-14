package org.wikipedia.feed.topread

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.getString
import org.wikipedia.feed.CommunityModuleContainer
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil

@Composable
fun TopReadModule(
    wikiSite: WikiSite,
    topRead: TopRead,
    pageOverflowContent: @Composable (Int) -> Unit,
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit,
    onPageClick: (PageSummary) -> Unit,
    onPageOverflowClick: (PageSummary, Int) -> Unit,
    onFooterClick: () -> Unit,
    onCardImpression: () -> Unit = {}
) {
    val maxTopReadItems = 5
    val context = LocalContext.current

    CommunityModuleContainer(
        wikiSite = wikiSite,
        titleResId = R.string.view_top_read_card_title,
        subTitleResId = R.string.view_top_read_card_description,
        backgroundColor = WikipediaTheme.colors.backgroundColor,
        onHideCardClick = onHideCardClick,
        onHideModuleClick = onHideModuleClick,
        onCardInView = onCardImpression
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            topRead.articles.take(maxTopReadItems).forEachIndexed { index, article ->
                val isTrendingUp = article.viewHistory?.takeLast(2)?.let { it.size < 2 || it[1].views > it[0].views } ?: true

                TopReadItem(
                    context = context,
                    wikiSite = wikiSite,
                    rank = index + 1,
                    isTrendingUp = isTrendingUp,
                    pageSummary = article,
                    pageOverflowContent = { pageOverflowContent(index) },
                    onClick = onPageClick,
                    onPageOverflowClick = { onPageOverflowClick(it, index) }
                )
            }
        }

        TextButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp),
            onClick = onFooterClick
        ) {
            Text(
                text = context.getString(wikiSite.languageCode, R.string.view_top_read_card_action),
                style = MaterialTheme.typography.labelLarge,
                color = WikipediaTheme.colors.progressiveColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
                contentDescription = context.getString(wikiSite.languageCode, R.string.view_top_read_card_action),
                tint = WikipediaTheme.colors.progressiveColor
            )
        }
    }
}

@Composable
fun TopReadItem(
    context: Context,
    wikiSite: WikiSite,
    rank: Int,
    isTrendingUp: Boolean,
    pageSummary: PageSummary,
    pageOverflowContent: @Composable () -> Unit,
    onClick: (PageSummary) -> Unit,
    onPageOverflowClick: (PageSummary) -> Unit
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
            Column(modifier = Modifier.weight(1f)) {
                Row {
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

                    HtmlText(
                        text = pageSummary.displayTitle,
                        color = WikipediaTheme.colors.primaryColor,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val trendingIcon = if (isTrendingUp) R.drawable.ic_trending_up_24dp else R.drawable.ic_trending_down_24dp
                    val trendingIconTint = if (isTrendingUp) WikipediaTheme.colors.successColor else WikipediaTheme.colors.destructiveColor
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(trendingIcon),
                        contentDescription = null,
                        tint = trendingIconTint
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = StringUtil.getPageViewText(context, pageSummary.views),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = context.getString(wikiSite.languageCode, R.string.view_top_read_card_pageviews_views_suffix),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
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

            Box(
                modifier = Modifier
                    .size(48.dp)
            ) {
                IconButton(
                    onClick = {
                        onPageOverflowClick(pageSummary)
                    },
                    content = {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                            contentDescription = context.getString(
                                wikiSite.languageCode,
                                R.string.menu_feed_overflow_label
                            ),
                            tint = WikipediaTheme.colors.placeholderColor
                        )
                    }
                )
                pageOverflowContent()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopReadCardPreview() {
    val article = PageSummary.preview()
    BaseTheme(currentTheme = Theme.LIGHT) {
        TopReadModule(
            wikiSite = WikiSite.preview(),
            topRead = TopRead(
                articles = listOf(article, article, article, article, article)
            ),
            pageOverflowContent = {},
            onFooterClick = {},
            onHideModuleClick = {},
            onPageClick = {},
            onPageOverflowClick = { _, _ -> }
        )
    }
}
