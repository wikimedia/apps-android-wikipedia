package org.wikipedia.feed.interests

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.CardVariation
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.BasedOnInterestCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
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
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeClick: (card: Card) -> Unit = {},
) {
    val context = LocalContext.current
    val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { pageIndex ->
        val card = module.cards[pageIndex] as BasedOnInterestCard
        val historyEntry = HistoryEntry(card.title, HistoryEntry.SOURCE_FEED_INTERESTS)
        val topic = ArticleTopics.all.find { it.topicId == card.interestTopic?.topicId }
        val footerTextParameter = if (topic != null) {
            context.getString(wikiSite.languageCode, topic.msgKey)
        } else card.interestArticle?.displayTitle

        ForYouCardContent(
            wikiSite = wikiSite,
            title = card.title,
            variation = CardVariation.entries[pageIndex % CardVariation.entries.size],
            backgroundColorIndex = backgroundColorIndex + pageIndex,
            module = module,
            card = module.cards[pageIndex],
            footerText = footerTextParameter?.let {
                context.getString(wikiSite.languageCode, R.string.explore_feed_because_of_interest, it)
            },
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
fun BasedOnInterestCardPreviewWithImage() {
    val card = BasedOnInterestCard(PageTitle.preview())
    BaseTheme(currentTheme = Theme.LIGHT) {
        BasedOnInterestModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.BasedOnInterest(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun BasedOnInterestCardPreviewNoImage() {
    val card = BasedOnInterestCard(PageTitle.preview(withThumbnail = false))
    BaseTheme(currentTheme = Theme.LIGHT) {
        BasedOnInterestModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.BasedOnInterest(0, 0, mutableListOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun BasedOnInterestCardPreviewTextOnlyWithImage() {
    val card = BasedOnInterestCard(PageTitle.preview())
    BaseTheme(currentTheme = Theme.LIGHT) {
        BasedOnInterestModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.BasedOnInterest(0, 0, mutableListOf(card, card, card, card))
        )
    }
}
