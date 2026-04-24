package org.wikipedia.feed.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.CommunityModuleHeader
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil
import org.wikipedia.views.imageservice.ImageService

@Composable
fun NewsModule(
    wikiSite: WikiSite,
    newsItems: List<NewsItem>,
    onNewsClick: (item: NewsItem) -> Unit = {},
    onHideModuleClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = WikipediaTheme.colors.paperColor)
    ) {

        val pagerState = rememberPagerState(pageCount = { newsItems.size })

        CommunityModuleHeader(
            wikiSite = wikiSite,
            titleResId = R.string.view_card_news_title,
            subTitleResId = R.string.explore_feed_in_the_news_subtitle,
            onHideModuleClick = onHideModuleClick
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            NewsItemContent(
                newsItem = newsItems[page],
                onItemClick = onNewsClick
            )
        }

        if (newsItems.size > 1) {
            PageIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                pagerState = pagerState
            )
        }
    }
}

@Composable
fun NewsItemContent(
    newsItem: NewsItem,
    onItemClick: (NewsItem) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable { onItemClick(newsItem) }
    ) {
        AsyncImage(
            model = newsItem.thumbUrl()?.let { ImageService.getRequest(LocalContext.current, url = it) },
            placeholder = ColorPainter(WikipediaTheme.colors.backgroundColor),
            error = ColorPainter(WikipediaTheme.colors.backgroundColor),
            contentDescription = newsItem.story,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(415.dp)
        )
        // Dark overlay on top of image:
        Box(
            modifier = Modifier.fillMaxWidth().height(415.dp).background(Color(0, 0, 0, 100))
        )

        if (newsItem.story.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WikipediaTheme.colors.paperColor.copy(alpha = 0.92f))
                    .padding(16.dp)
            ) {
                Text(
                    text = StringUtil.fromHtml(removeItalicParenthetical(newsItem.story)).toString(),
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun removeItalicParenthetical(text: String): String {
    return text.replace("<i.*?>(.*?)</i>".toRegex(), "")
}

@Preview(showBackground = true)
@Composable
fun NewsCardPreview() {
    val pageSummary = PageSummary(
        displayTitle = "Dog and Cat",
        prefixTitle = "Dog_and_Cat",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        thumbnail = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Commons-logo.svg/1200px-Commons-logo.svg.png",
        lang = "en"
    )
    BaseTheme(currentTheme = Theme.LIGHT) {
        NewsModule(
            wikiSite = WikiSite.preview(),
            newsItems = listOf(
                NewsItem(
                    story = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                    links = listOf(pageSummary)
                ),
                NewsItem(
                    story = "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                    links = listOf(pageSummary)
                ),
                NewsItem(
                    story = "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                    links = listOf(pageSummary)
                )
            )
        )
    }
}
