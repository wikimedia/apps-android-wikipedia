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
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeInterestsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { pageIndex ->
        val entry = (module.cards[pageIndex] as ContinueReadingCard).entry
        ForYouCardContent(
            wikiSite = wikiSite,
            entry = entry,
            variation = CardVariation.entries[pageIndex % CardVariation.entries.size],
            backgroundColorIndex = backgroundColorIndex + pageIndex,
            module = module,
            card = module.cards[pageIndex],
            footerIcon = painterResource(if (entry.source == HistoryEntry.SOURCE_READING_LIST) R.drawable.ic_bookmark_border_white_24dp else R.drawable.ic_read_more_24dp),
            footerText = context.getString(wikiSite.languageCode, if (entry.source == HistoryEntry.SOURCE_READING_LIST) R.string.explore_feed_from_reading_list else R.string.app_shortcuts_continue_reading),
            onPageClick = onPageClick,
            onHideCardClick = onHideCardClick,
            onHideModuleClick = onHideModuleClick,
            onCustomizeInterestsClick = onCustomizeInterestsClick
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
