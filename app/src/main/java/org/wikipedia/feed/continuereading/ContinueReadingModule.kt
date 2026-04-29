package org.wikipedia.feed.continuereading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil
import org.wikipedia.views.imageservice.ImageService
import kotlin.random.Random

@Composable
fun ContinueReadingModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    cards: List<ContinueReadingCard>,
    onPageClick: (item: PageSummary) -> Unit = {},
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
    ) {

        val pagerState = rememberPagerState(pageCount = { cards.size })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            ContinueReadingCardContent(
                wikiSite = wikiSite,
                card = cards[page],
                onPageClick = onPageClick
            )
        }

        if (cards.size > 1) {
            PageIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 16.dp),
                pagerState = pagerState,
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ContinueReadingCardContent(
    wikiSite: WikiSite,
    card: ContinueReadingCard,
    onPageClick: (PageSummary) -> Unit = {},
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onPageClick(PageSummary()) } //TODO
    ) {
        if (card.entry.title.thumbUrl.isNullOrEmpty()) {
            val color = colorResource(listOf(R.color.maroon800, R.color.purple800, R.color.pink800).random(Random(card.entry.title.hashCode())))
            Box(
                modifier = Modifier.fillMaxSize().background(color)
            )
        } else {
            AsyncImage(
                model = card.entry.title.thumbUrl?.let { ImageService.getRequest(LocalContext.current, url = it) },
                placeholder = ColorPainter(Color.Black),
                error = ColorPainter(Color.DarkGray),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (card.entry.title.thumbUrl.isNullOrEmpty()) {
            Text(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(32.dp),
                text = StringUtil.fromHtml(card.entry.title.displayText).toString(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )

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
                    text = StringUtil.fromHtml(card.entry.title.displayText).toString(),
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }


        } else {

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.18f to Color.Black.copy(alpha = 0.05f),
                            0.38f to Color.Black.copy(alpha = 0.15f),
                            0.58f to Color.Black.copy(alpha = 0.30f),
                            0.76f to Color.Black.copy(alpha = 0.50f),
                            0.90f to Color.Black.copy(alpha = 0.7f),
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    ))
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HtmlText(
                        modifier = Modifier.weight(1f).padding(start = 16.dp, top = 8.dp),
                        text = card.entry.title.displayText,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 3
                    )
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = {
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                            contentDescription = context.getString(wikiSite.languageCode, R.string.menu_feed_overflow_label),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                val text = card.entry.title.extract ?: card.entry.title.description
                text?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = it,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Preview
@Composable
fun ContinueReadingCardPreviewWithImage() {
    val wikiSite = WikiSite.preview()
    val entry = HistoryEntry(
        title = PageTitle(
            text = "Test Article",
            displayText = "Test Article",
            wiki = WikiSite.preview(),
            description = "This is a test article",
            extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            thumbUrl = "https://example.com/thumb.jpg"
        ), source = HistoryEntry.SOURCE_HISTORY
    )
    val card = ContinueReadingCard(entry)
    BaseTheme(currentTheme = Theme.LIGHT) {
        ContinueReadingModule(
            wikiSite = wikiSite,
            cards = listOf(card, card, card, card)
        )
    }
}

@Preview
@Composable
fun ContinueReadingCardPreviewNoImage() {
    val wikiSite = WikiSite.preview()
    val entry = HistoryEntry(
        title = PageTitle(
            text = "Test Article",
            displayText = "Test Article",
            wiki = WikiSite.preview(),
            description = "This is a test article",
            thumbUrl = null
        ), source = HistoryEntry.SOURCE_HISTORY
    )
    val card = ContinueReadingCard(entry)
    BaseTheme(currentTheme = Theme.LIGHT) {
        ContinueReadingModule(
            wikiSite = wikiSite,
            cards = listOf(card, card, card, card)
        )
    }
}
