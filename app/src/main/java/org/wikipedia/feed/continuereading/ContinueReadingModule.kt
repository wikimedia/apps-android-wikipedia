package org.wikipedia.feed.continuereading

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
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ContinueReadingCard
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import kotlin.math.abs

@Composable
fun ContinueReadingModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.ContinueReading,
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
    ) { pageIndex ->
        val card = (module.cards[pageIndex] as ContinueReadingCard)
        val historyEntry = HistoryEntry(card.title, HistoryEntry.SOURCE_FEED_CONTINUE_READING)
        ForYouCardContent(
            wikiSite = wikiSite,
            title = card.title,
            variation = CardVariation.entries[pageIndex % CardVariation.entries.size],
            backgroundColorIndex = backgroundColorIndex + pageIndex,
            module = module,
            card = module.cards[pageIndex],
            footerIcon = painterResource(if (card.source == HistoryEntry.SOURCE_READING_LIST) R.drawable.ic_bookmark_border_white_24dp else R.drawable.ic_read_more_24dp),
            footerText = context.getString(wikiSite.languageCode, if (card.source == HistoryEntry.SOURCE_READING_LIST) R.string.explore_feed_from_reading_list else R.string.app_shortcuts_continue_reading),
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
fun ContinueReadingCardPreviewWithImage() {
    val card = ContinueReadingCard(PageTitle.preview(), HistoryEntry.SOURCE_HISTORY)
    BaseTheme(currentTheme = Theme.LIGHT) {
        ContinueReadingModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.ContinueReading(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun ContinueReadingCardPreviewNoImage() {
    val card = ContinueReadingCard(PageTitle.preview(withThumbnail = false), HistoryEntry.SOURCE_HISTORY)
    BaseTheme(currentTheme = Theme.LIGHT) {
        ContinueReadingModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.ContinueReading(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun ContinueReadingCardPreviewTextOnlyWithImage() {
    val card = ContinueReadingCard(PageTitle.preview(), HistoryEntry.SOURCE_HISTORY)
    BaseTheme(currentTheme = Theme.LIGHT) {
        ContinueReadingModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.ContinueReading(0, 0, mutableListOf(card, card, card, card))
        )
    }
}
