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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.model.Card
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.imageservice.ImageService
import kotlin.random.Random

@Composable
fun ContinueReadingModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.ContinueReading,
    onPageClick: (item: HistoryEntry) -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: Card) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
    ) {
        val pagerState = rememberPagerState(pageCount = { module.cards.size })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            ContinueReadingCardContent(
                wikiSite = wikiSite,
                module = module,
                card = module.cards[page] as ContinueReadingCard,
                onPageClick = onPageClick,
                onHideCardClick = onHideCardClick,
                onHideModuleClick = onHideModuleClick
            )
        }

        if (module.cards.size > 1) {
            PageIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 16.dp),
                pagerState = pagerState,
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ContinueReadingCardContent(
    wikiSite: WikiSite,
    module: ForYouModule.ContinueReading,
    card: ContinueReadingCard,
    onPageClick: (HistoryEntry) -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: Card) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onPageClick(HistoryEntry(card.entry.title, HistoryEntry.SOURCE_FEED_CONTINUE_READING)) }
    ) {
        if (card.entry.title.thumbUrl.isNullOrEmpty()) {
            val color = colorResource(listOf(R.color.maroon800, R.color.purple800, R.color.pink800).random(Random(card.entry.title.hashCode())))
            Box(
                modifier = Modifier.fillMaxSize().background(color)
            )
        } else {
            AsyncImage(
                model = card.entry.title.thumbUrl?.let { ImageService.getRequest(LocalContext.current,
                    url = ImageUrlUtil.getUrlForPreferredSize(it, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)) },
                placeholder = ColorPainter(Color.Black),
                error = ColorPainter(Color.White),
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
            ) {
                Column(
                    modifier = Modifier.background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.18f to Color.Black.copy(alpha = 0.05f),
                                0.38f to Color.Black.copy(alpha = 0.15f),
                                0.58f to Color.Black.copy(alpha = 0.30f),
                                0.76f to Color.Black.copy(alpha = 0.50f),
                                0.90f to Color.Black.copy(alpha = 0.7f),
                                1.0f to Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
                ) {
                    Spacer(modifier = Modifier.height(100.dp))
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
                                overflowMenuExpanded = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                                contentDescription = context.getString(
                                    wikiSite.languageCode,
                                    R.string.menu_feed_overflow_label
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        ContinueReadingCardDropdownMenu(
                            expanded = overflowMenuExpanded,
                            wikiSite = wikiSite,
                            onDismiss = { overflowMenuExpanded = false },
                            onHideCardClick = { onHideCardClick(module, card) },
                            onHideModuleClick = onHideModuleClick
                        )
                    }
                }
                Column(
                    modifier = Modifier.background(color = Color.Black.copy(alpha = 0.80f))
                        .padding(bottom = 40.dp)
                ) {
                    val text = card.entry.title.extract ?: card.entry.title.description
                    text?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        HtmlText(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(if (card.entry.source == HistoryEntry.SOURCE_READING_LIST) R.drawable.ic_bookmark_border_white_24dp else R.drawable.ic_read_more_24dp),
                                contentDescription = context.getString(
                                    wikiSite.languageCode,
                                    R.string.menu_feed_overflow_label
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                text = context.getString(wikiSite.languageCode, if (card.entry.source == HistoryEntry.SOURCE_READING_LIST) R.string.explore_feed_from_reading_list else R.string.app_shortcuts_continue_reading),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueReadingCardDropdownMenu(
    expanded: Boolean,
    wikiSite: WikiSite,
    onDismiss: () -> Unit = {},
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit = {}
) {
    val context = LocalContext.current
    DropdownMenu(
        offset = DpOffset(x = (-16).dp, y = 0.dp),
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = WikipediaTheme.colors.paperColor
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_visibility_off_24dp),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.secondaryColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = context.getString(wikiSite.languageCode, R.string.menu_feed_card_dismiss),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            onClick = {
                onHideCardClick()
                onDismiss()
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_visibility_off_24dp),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.secondaryColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = context.getString(wikiSite.languageCode, R.string.explore_feed_header_overflow_hide_module_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            onClick = {
                onHideModuleClick()
                onDismiss()
            }
        )
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
            module = ForYouModule.ContinueReading(0, mutableListOf(card, card, card, card))
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
            module = ForYouModule.ContinueReading(0, mutableListOf(card, card, card, card))
        )
    }
}
