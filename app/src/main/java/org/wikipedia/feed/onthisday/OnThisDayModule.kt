package org.wikipedia.feed.onthisday

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.getString
import org.wikipedia.feed.CommunityModuleHeader
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil
import org.wikipedia.views.imageservice.ImageService

@Composable
fun OnThisDayModule(
    wikiSite: WikiSite,
    events: List<OnThisDay.Event>,
    onModuleClick: () -> Unit = {},
    onPageClick: (page: PageSummary) -> Unit = {},
    onOverflowClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = WikipediaTheme.colors.backgroundColor)
    ) {
        CommunityModuleHeader(
            wikiSite = wikiSite,
            titleResId = R.string.on_this_day_card_title,
            subTitleResId = R.string.explore_feed_on_this_day_subtitle,
            onOverflowClick = onOverflowClick
        )

        events.forEachIndexed { index, event ->
            EventRow(
                context = context,
                wikiSite = wikiSite,
                isFirst = index == 0,
                event = event,
                onPageClick = onPageClick
            )
            if (index < events.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EventRow(
    context: Context,
    wikiSite: WikiSite,
    event: OnThisDay.Event,
    isFirst: Boolean,
    onPageClick: (page: PageSummary) -> Unit = {},
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(start = 20.dp, top = 6.dp)
                .width(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isFirst) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(WikipediaTheme.colors.progressiveColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .border(2.dp, WikipediaTheme.colors.progressiveColor, CircleShape)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(WikipediaTheme.colors.progressiveColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.year.toString(),
                fontWeight = FontWeight.Bold,
                color = WikipediaTheme.colors.progressiveColor
            )
            Text(
                text = "${event.year} years ago",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.text,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                event.pages.forEach { page ->
                    OnThisDayPageItem(
                        context,
                        wikiSite,
                        page,
                        onClick = onPageClick,
                        onMoreClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun OnThisDayPageItem(
    context: Context,
    wikiSite: WikiSite,
    pageSummary: PageSummary,
    onClick: (PageSummary) -> Unit,
    onMoreClick: (PageSummary) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WikipediaTheme.colors.borderColor),
        color = WikipediaTheme.colors.paperColor,
        modifier = Modifier.width(260.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                HtmlText(
                    text = pageSummary.displayTitle,
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (!pageSummary.description.isNullOrEmpty()) {
                    Text(
                        text = pageSummary.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = WikipediaTheme.colors.secondaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    onMoreClick(pageSummary)
                },
                content = {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                        contentDescription = context.getString(wikiSite.languageCode, R.string.menu_feed_overflow_label),
                        tint = WikipediaTheme.colors.placeholderColor
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun OnThisDayPageItemPreview() {
    val pageSummary = PageSummary(
        displayTitle = "Dog and Cat",
        prefixTitle = "Dog_and_Cat",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        thumbnail = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Commons-logo.svg/1200px-Commons-logo.svg.png",
        lang = "en"
    )
    BaseTheme(currentTheme = Theme.LIGHT) {
        OnThisDayPageItem(
            LocalContext.current,
            wikiSite = WikiSite.preview(),
            pageSummary = pageSummary,
            onClick = {},
            onMoreClick = {}
        )
    }
}

@Preview
@Composable
fun OnThisDayModulePreview() {
    val pageSummary = PageSummary(
        displayTitle = "Lorem ipsum",
        prefixTitle = "Lorem_ipsum",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        thumbnail = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Commons-logo.svg/1200px-Commons-logo.svg.png",
        lang = "en"
    )

    val event = OnThisDay.Event(
        year = 2023,
        text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        pages = listOf(pageSummary, pageSummary, pageSummary)
    )

    BaseTheme(currentTheme = Theme.LIGHT) {
        OnThisDayModule(
            wikiSite = WikiSite.preview(),
            events = listOf(event, event)
        )
    }
}
