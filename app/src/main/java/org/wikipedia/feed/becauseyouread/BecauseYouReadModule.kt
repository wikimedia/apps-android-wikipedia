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
import org.wikipedia.feed.model.Card
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import kotlin.math.abs

@Composable
fun BecauseYouReadModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.BecauseYouRead,
    onPageClick: (item: HistoryEntry) -> Unit = {},
    onShareClick: (entry: HistoryEntry) -> Unit = {},
    onSaveClick: (entry: HistoryEntry) -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: Card) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeInterestsClick: () -> Unit = {}
) {
    val context = LocalContext.current
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
            footerText = context.getString(wikiSite.languageCode, R.string.explore_feed_because_you_read, card.sourceDisplayTitle),
            onPageClick = onPageClick,
            onShareClick = onShareClick,
            onSaveClick = onSaveClick,
            onHideCardClick = onHideCardClick,
            onHideModuleClick = onHideModuleClick,
            onCustomizeInterestsClick = onCustomizeInterestsClick
        )
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
    val card = BecauseYouReadCard(entry, sourceDisplayTitle = "<i>Test Article</i>")
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
