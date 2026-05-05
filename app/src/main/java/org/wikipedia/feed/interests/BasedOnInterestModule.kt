package org.wikipedia.feed.interests

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.CardVariation
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.topics.ArticleTopics
import kotlin.math.abs

@Composable
fun BasedOnInterestModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.BasedOnInterest,
    onPageClick: (item: HistoryEntry) -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: Card) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeInterestsClick: () -> Unit = {},
) {
    val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { pageIndex ->
        val card = module.cards[pageIndex] as BasedOnInterestCard
        val topic = ArticleTopics.all.find { it.topicId == card.interestTopic?.topicId }

        ForYouCardContent(
            wikiSite = wikiSite,
            entry = card.entry,
            variation = CardVariation.entries[pageIndex % CardVariation.entries.size],
            backgroundColorIndex = backgroundColorIndex + pageIndex,
            module = module,
            card = module.cards[pageIndex],
            footerText = (if (topic != null) stringResource(topic.msgKey) else card.interestArticle?.displayTitle)?.let {
                stringResource(R.string.explore_feed_because_of_interest, it)
            },
            onPageClick = onPageClick,
            onHideCardClick = onHideCardClick,
            onHideModuleClick = onHideModuleClick,
            onCustomizeInterestsClick = onCustomizeInterestsClick
        )
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
