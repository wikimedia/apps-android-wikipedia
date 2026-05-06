package org.wikipedia.feed.becauseyouread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil
import kotlin.math.abs

@Composable
fun BecauseYouReadModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.BecauseYouRead,
    onPageClick: (item: HistoryEntry) -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: Card) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeInterestsClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
    ) {
        val context = LocalContext.current
        val pagerState = rememberPagerState(pageCount = { module.cards.size })
        val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

        ForYouModulePager(
            modifier = modifier,
            module = module,
            onCardInView = onCardInView
        ) { page ->
            val card = (module.cards[page] as BecauseYouReadCard)
            ForYouCardContent(
                wikiSite = wikiSite,
                entry = card.entry,
                variation = CardVariation.entries[page % CardVariation.entries.size],
                backgroundColorIndex = backgroundColorIndex + page,
                module = module,
                card = module.cards[page],
                footerIcon = painterResource(R.drawable.ic_history_24),
                footerText = context.getString(wikiSite.languageCode, R.string.explore_feed_because_you_read, StringUtil.removeHTMLTags(card.sourceDisplayTitle)),
                onPageClick = onPageClick,
                onHideCardClick = onHideCardClick,
                onHideModuleClick = onHideModuleClick,
                onCustomizeInterestsClick = onCustomizeInterestsClick
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
fun BecauseYouReadCardPreviewWithImage() {
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
    val card = BecauseYouReadCard(entry, sourceDisplayTitle = "Test Article")
    BaseTheme(currentTheme = Theme.LIGHT) {
        BecauseYouReadModule(
            wikiSite = wikiSite,
            module = ForYouModule.BecauseYouRead(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun BecauseYouReadCardPreviewNoImage() {
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
    val card = BecauseYouReadCard(entry, sourceDisplayTitle = "Test Article")
    BaseTheme(currentTheme = Theme.LIGHT) {
        BecauseYouReadModule(
            wikiSite = wikiSite,
            module = ForYouModule.BecauseYouRead(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun BecauseYouReadCardPreviewTextOnlyWithImage() {
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
    val card = BecauseYouReadCard(entry, sourceDisplayTitle = "Test Article")
    BaseTheme(currentTheme = Theme.LIGHT) {
        BecauseYouReadModule(
            wikiSite = wikiSite,
            module = ForYouModule.BecauseYouRead(0, 0, mutableListOf(card, card, card, card))
        )
    }
}
