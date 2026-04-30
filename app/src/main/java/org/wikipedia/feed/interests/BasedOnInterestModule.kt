package org.wikipedia.feed.interests

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.CardVariation
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.model.Card
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import kotlin.math.abs

@Composable
fun BasedOnInterestModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.BasedOnInterest,
    onPageClick: (item: HistoryEntry) -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: Card) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
    ) {
        val pagerState = rememberPagerState(pageCount = { module.cards.size })
        val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val card = module.cards[page] as BasedOnInterestCard

            ForYouCardContent(
                wikiSite = wikiSite,
                entry = card.entry,
                variation = CardVariation.entries[page % CardVariation.entries.size],
                backgroundColorIndex = backgroundColorIndex + page,
                module = module,
                card = module.cards[page],
                footerText = stringResource(R.string.explore_feed_because_of_interest, card.interestTopic?.topicLabel.orEmpty()),
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
fun BasedOnInterestCardPreviewWithImage() {
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
    val card = BasedOnInterestCard(entry)
    BaseTheme(currentTheme = Theme.LIGHT) {
        BasedOnInterestModule(
            wikiSite = wikiSite,
            module = ForYouModule.BasedOnInterest(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun BasedOnInterestCardPreviewNoImage() {
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
    val card = BasedOnInterestCard(entry)
    BaseTheme(currentTheme = Theme.LIGHT) {
        BasedOnInterestModule(
            wikiSite = wikiSite,
            module = ForYouModule.BasedOnInterest(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun BasedOnInterestCardPreviewTextOnlyWithImage() {
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
    val card = BasedOnInterestCard(entry)
    BaseTheme(currentTheme = Theme.LIGHT) {
        BasedOnInterestModule(
            wikiSite = wikiSite,
            module = ForYouModule.BasedOnInterest(0, 0, mutableListOf(card, card, card, card))
        )
    }
}
