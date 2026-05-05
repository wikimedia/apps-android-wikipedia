package org.wikipedia.feed.continuereading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.CardVariation
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.model.Card
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import kotlin.math.abs

@Composable
fun ContinueReadingModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.ContinueReading,
    onPageClick: (item: HistoryEntry) -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: Card) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
    ) {
        val pagerState = rememberPagerState(pageCount = { module.cards.size })
        val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val entry = (module.cards[page] as ContinueReadingCard).entry
            ForYouCardContent(
                wikiSite = wikiSite,
                entry = entry,
                variation = CardVariation.entries[page % CardVariation.entries.size],
                backgroundColorIndex = backgroundColorIndex + page,
                module = module,
                card = module.cards[page],
                footerIcon = painterResource(if (entry.source == HistoryEntry.SOURCE_READING_LIST) R.drawable.ic_bookmark_border_white_24dp else R.drawable.ic_read_more_24dp),
                footerText = context.getString(wikiSite.languageCode, if (entry.source == HistoryEntry.SOURCE_READING_LIST) R.string.explore_feed_from_reading_list else R.string.app_shortcuts_continue_reading),
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
                inactiveColor = Color.White.copy(alpha = 0.5f)
            )
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
            module = ForYouModule.ContinueReading(0, 0, mutableListOf(card, card, card, card))
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
            extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            thumbUrl = null
        ), source = HistoryEntry.SOURCE_HISTORY
    )
    val card = ContinueReadingCard(entry)
    BaseTheme(currentTheme = Theme.LIGHT) {
        ContinueReadingModule(
            wikiSite = wikiSite,
            module = ForYouModule.ContinueReading(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun ContinueReadingCardPreviewTextOnlyWithImage() {
    val wikiSite = WikiSite.preview()
    val entry = HistoryEntry(
        title = PageTitle(
            text = "Test Article",
            displayText = "Test Article",
            wiki = WikiSite.preview(),
            description = "This is a test article",
            extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            thumbUrl = "test.jpg"
        ), source = HistoryEntry.SOURCE_HISTORY
    )
    val card = ContinueReadingCard(entry)
    BaseTheme(currentTheme = Theme.LIGHT) {
        ContinueReadingModule(
            wikiSite = wikiSite,
            module = ForYouModule.ContinueReading(0, 0, mutableListOf(card, card, card, card))
        )
    }
}
