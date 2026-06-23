package org.wikipedia.feed.becauseyouread

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.CardVariation
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.BecauseYouReadCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import kotlin.math.abs

@Composable
fun BecauseYouReadModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.BecauseYouRead,
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeClick: (card: Card) -> Unit = {}
) {
    val context = LocalContext.current
    val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { page ->
        val card = (module.cards[page] as BecauseYouReadCard)
        val historyEntry = HistoryEntry(card.title, HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ)
        ForYouCardContent(
            wikiSite = wikiSite,
            title = card.title,
            variation = CardVariation.entries[page % CardVariation.entries.size],
            backgroundColorIndex = backgroundColorIndex + page,
            module = module,
            card = module.cards[page],
            footerIcon = painterResource(R.drawable.ic_history_24),
            footerText = context.getString(wikiSite.languageCode, R.string.explore_feed_because_you_read, card.sourceDisplayTitle),
            onPageClick = { onPageClick(card, historyEntry) },
            onShareClick = { onPageShareClick(card, historyEntry) },
            onSaveClick = { onPageBookmarkClick(card, historyEntry) },
            onHideCardClick = onHideCardClick,
            onHideModuleClick = onHideModuleClick,
            onCustomizeClick = { onCustomizeClick(card) }
        )
    }
}

@Preview
@Composable
fun BecauseYouReadCardPreviewWithImage() {
    val card = BecauseYouReadCard(PageTitle.preview(), sourceDisplayTitle = "<i>Test Article</i>")
    BaseTheme(currentTheme = Theme.LIGHT) {
        BecauseYouReadModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.BecauseYouRead(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun BecauseYouReadCardPreviewNoImage() {
    val card = BecauseYouReadCard(PageTitle.preview(withThumbnail = false), sourceDisplayTitle = "Test Article")
    BaseTheme(currentTheme = Theme.LIGHT) {
        BecauseYouReadModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.BecauseYouRead(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun BecauseYouReadCardPreviewTextOnlyWithImage() {
    val card = BecauseYouReadCard(PageTitle.preview(), sourceDisplayTitle = "Test Article")
    BaseTheme(currentTheme = Theme.LIGHT) {
        BecauseYouReadModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.BecauseYouRead(0, 0, mutableListOf(card, card, card, card))
        )
    }
}
