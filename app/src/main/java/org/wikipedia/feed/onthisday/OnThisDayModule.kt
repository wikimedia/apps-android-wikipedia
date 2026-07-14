package org.wikipedia.feed.onthisday

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
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
import org.wikipedia.util.DateUtil

@Composable
fun OnThisDayModule(
    wikiSite: WikiSite,
    events: List<OnThisDay.Event>,
    pageOverflowContent: @Composable (eventIndex: Int, itemIndex: Int) -> Unit,
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit = {},
    onPageClick: (page: PageSummary) -> Unit = {},
    onPageOverflowClick: (PageSummary, Int, Int) -> Unit = { _, _, _ -> },
    onFooterClick: () -> Unit = {},
    onCardImpression: () -> Unit = {}
) {
    val context = LocalContext.current

    CommunityModuleContainer(
        wikiSite = wikiSite,
        titleResId = R.string.on_this_day_card_title,
        subTitleResId = R.string.explore_feed_on_this_day_subtitle,
        backgroundColor = WikipediaTheme.colors.backgroundColor,
        onHideCardClick = onHideCardClick,
        onHideModuleClick = onHideModuleClick,
        onCardInView = onCardImpression
    ) {
        events.forEachIndexed { eventIndex, event ->
            EventRow(
                context = context,
                wikiSite = wikiSite,
                isFirst = eventIndex == 0,
                event = event,
                pageOverflowContent = { pageOverflowContent(eventIndex, it) },
                onPageClick = onPageClick,
                onPageOverflowClick = { pageSummary, itemIndex ->
                    onPageOverflowClick(pageSummary, eventIndex, itemIndex)
                }
            )
        }

        TextButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp),
            onClick = onFooterClick
        ) {
            Text(
                text = context.getString(wikiSite.languageCode, R.string.more_events_text),
                style = MaterialTheme.typography.labelLarge,
                color = WikipediaTheme.colors.progressiveColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
                contentDescription = context.getString(wikiSite.languageCode, R.string.more_events_text),
                tint = WikipediaTheme.colors.progressiveColor
            )
        }
    }
}

@Composable
private fun EventRow(
    context: Context,
    wikiSite: WikiSite,
    event: OnThisDay.Event,
    isFirst: Boolean,
    pageOverflowContent: @Composable (Int) -> Unit,
    onPageClick: (page: PageSummary) -> Unit = {},
    onPageOverflowClick: (PageSummary, Int) -> Unit = { _, _ -> },
) {
    val containerSize = remember { mutableStateOf(DpSize(0.dp, 0.dp)) }
    val localDensity = LocalDensity.current

    Box(
        modifier = Modifier.fillMaxWidth().onSizeChanged {
            containerSize.value = DpSize((it.width / localDensity.density).dp, (it.height / localDensity.density).dp)
        },
    ) {

        Column(
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 21.dp)
                    .width(1.dp)
                    .weight(1f)
                    .background(WikipediaTheme.colors.borderColor)
            )
        }

        if (isFirst) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(WikipediaTheme.colors.progressiveColor)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 24.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .border(1.dp, WikipediaTheme.colors.progressiveColor, CircleShape)
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

        Column(modifier = Modifier.fillMaxWidth()) {
            if (!isFirst) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                modifier = Modifier.padding(start = 36.dp),
                text = event.year.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.W500
                ),
                color = WikipediaTheme.colors.progressiveColor
            )
            Text(
                modifier = Modifier.padding(start = 36.dp, top = 8.dp),
                text = DateUtil.getYearDifferenceString(context, event.year, wikiSite.languageCode),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.W600
                ),
                color = WikipediaTheme.colors.primaryColor
            )
            Text(
                modifier = Modifier.padding(start = 36.dp, end = 16.dp, top = 8.dp),
                text = event.text,
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.primaryColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.width(20.dp))
                event.pages.forEachIndexed { index, page ->
                    OnThisDayPageItem(
                        context = context,
                        viewPortSize = containerSize.value,
                        wikiSite = wikiSite,
                        pageSummary = page,
                        pageOverflowContent = { pageOverflowContent(index) },
                        onPageClick = onPageClick,
                        onPageOverflowClick = { onPageOverflowClick(page, index) }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OnThisDayPageItem(
    context: Context,
    viewPortSize: DpSize,
    wikiSite: WikiSite,
    pageSummary: PageSummary,
    pageOverflowContent: @Composable () -> Unit,
    onPageClick: (PageSummary) -> Unit,
    onPageOverflowClick: (PageSummary) -> Unit = {},
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WikipediaTheme.colors.borderColor),
        color = WikipediaTheme.colors.paperColor,
        modifier = Modifier.width(min(viewPortSize.width - 100.dp, 480.dp)),
        onClick = { onPageClick(pageSummary) }
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                HtmlText(
                    text = pageSummary.displayTitle,
                    color = WikipediaTheme.colors.primaryColor,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.W600
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
                        .padding(start = 8.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box {
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
                            tint = WikipediaTheme.colors.secondaryColor
                        )
                    }
                )
                pageOverflowContent()
            }
        }
    }
}

@Preview
@Composable
fun OnThisDayPageItemPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        OnThisDayPageItem(
            LocalContext.current,
            viewPortSize = DpSize(360.dp, 80.dp),
            wikiSite = WikiSite.preview(),
            pageSummary = PageSummary.preview(),
            pageOverflowContent = {},
            onPageClick = {},
            onPageOverflowClick = {}
        )
    }
}

@Preview
@Composable
fun OnThisDayModulePreview() {
    val event = OnThisDay.Event(
        year = 2023,
        text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        pages = listOf(PageSummary.preview(), PageSummary.preview(), PageSummary.preview())
    )

    BaseTheme(currentTheme = Theme.LIGHT) {
        OnThisDayModule(
            wikiSite = WikiSite.preview(),
            pageOverflowContent = { _, _ -> },
            events = listOf(event, event)
        )
    }
}
